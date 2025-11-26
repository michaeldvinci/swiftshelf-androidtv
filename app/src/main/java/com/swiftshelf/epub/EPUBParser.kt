package com.swiftshelf.epub

import org.jsoup.Jsoup
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

/**
 * EPUB parser that extracts content from EPUB files.
 * Uses JSoup for HTML parsing and Java's ZipInputStream for decompression.
 */
object EPUBParser {

    data class EPUBContent(
        val title: String,
        val chapters: List<Chapter>,
        val spineItems: List<SpineItem>,
        val tocChapters: List<TOCChapter>
    )

    data class Chapter(
        val title: String?,
        val htmlContent: String
    )

    data class SpineItem(
        val htmlContent: String,
        val href: String
    )

    data class TOCChapter(
        val title: String,
        val href: String,
        val fragmentId: String?
    )

    /**
     * Parse EPUB data and extract content
     */
    fun parse(data: ByteArray): EPUBContent {
        // Extract all files from the EPUB zip
        val files = mutableMapOf<String, ByteArray>()

        ZipInputStream(ByteArrayInputStream(data)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val buffer = ByteArrayOutputStream()
                    val bytes = ByteArray(4096)
                    var len: Int
                    while (zis.read(bytes).also { len = it } != -1) {
                        buffer.write(bytes, 0, len)
                    }
                    files[entry.name] = buffer.toByteArray()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        // Parse container.xml to find content.opf location
        val containerXml = files["META-INF/container.xml"]?.toString(Charsets.UTF_8)
            ?: throw IllegalStateException("No container.xml found in EPUB")

        val contentOpfPath = parseContainer(containerXml)
        val contentOpfDir = contentOpfPath.substringBeforeLast("/", "")

        // Parse content.opf to get spine order and metadata
        val contentOpfData = files[contentOpfPath]?.toString(Charsets.UTF_8)
            ?: throw IllegalStateException("No content.opf found at $contentOpfPath")

        val (title, manifest, spineIds, ncxPath) = parseContentOpf(contentOpfData)

        // Build spine items (all HTML files in reading order)
        val spineItems = spineIds.mapNotNull { id ->
            val href = manifest[id] ?: return@mapNotNull null
            val fullPath = if (contentOpfDir.isNotEmpty()) "$contentOpfDir/$href" else href
            val htmlContent = files[fullPath]?.toString(Charsets.UTF_8) ?: return@mapNotNull null
            SpineItem(htmlContent = htmlContent, href = href)
        }

        // Parse TOC from NCX or Nav document
        val tocChapters = ncxPath?.let { ncx ->
            val fullNcxPath = if (contentOpfDir.isNotEmpty()) "$contentOpfDir/$ncx" else ncx
            files[fullNcxPath]?.toString(Charsets.UTF_8)?.let { ncxContent ->
                parseTOC(ncxContent)
            }
        } ?: emptyList()

        // Create chapters from spine items for backward compatibility
        val chapters = spineItems.map { Chapter(title = null, htmlContent = it.htmlContent) }

        return EPUBContent(
            title = title,
            chapters = chapters,
            spineItems = spineItems,
            tocChapters = tocChapters
        )
    }

    /**
     * Parse container.xml to find content.opf path
     */
    private fun parseContainer(xml: String): String {
        val regex = """full-path="([^"]+)"""".toRegex()
        return regex.find(xml)?.groupValues?.get(1)
            ?: throw IllegalStateException("Could not find content.opf path in container.xml")
    }

    /**
     * Parse content.opf to extract metadata, manifest, and spine
     * Returns: (title, manifest map, spine id list, ncx path)
     */
    private fun parseContentOpf(xml: String): ContentOpfResult {
        val doc = Jsoup.parse(xml)

        // Extract title
        val title = doc.select("dc\\:title, title").firstOrNull()?.text() ?: "Unknown"

        // Build manifest (id -> href mapping)
        val manifest = mutableMapOf<String, String>()
        var ncxPath: String? = null

        doc.select("item").forEach { item ->
            val id = item.attr("id")
            val href = item.attr("href")
            val mediaType = item.attr("media-type")
            val properties = item.attr("properties")

            if (id.isNotEmpty() && href.isNotEmpty()) {
                manifest[id] = href

                // Look for NCX (EPUB 2) or Nav (EPUB 3)
                if (mediaType == "application/x-dtbncx+xml" || properties.contains("nav")) {
                    ncxPath = href
                }
            }
        }

        // Extract spine order
        val spineIds = doc.select("itemref").mapNotNull { itemref ->
            val idref = itemref.attr("idref")
            idref.takeIf { it.isNotEmpty() }
        }

        return ContentOpfResult(title, manifest, spineIds, ncxPath)
    }

    private data class ContentOpfResult(
        val title: String,
        val manifest: Map<String, String>,
        val spineIds: List<String>,
        val ncxPath: String?
    )

    /**
     * Parse TOC from NCX (EPUB 2) or Nav (EPUB 3) document
     */
    private fun parseTOC(content: String): List<TOCChapter> {
        val chapters = mutableListOf<TOCChapter>()

        // Check if it's NCX or Nav format
        if (content.contains("<ncx", ignoreCase = true)) {
            // EPUB 2 NCX format
            parseNCX(content, chapters)
        } else if (content.contains("<nav", ignoreCase = true)) {
            // EPUB 3 Nav format
            parseNav(content, chapters)
        }

        return chapters
    }

    /**
     * Parse EPUB 2 NCX format
     */
    private fun parseNCX(content: String, chapters: MutableList<TOCChapter>) {
        val doc = Jsoup.parse(content)

        doc.select("navPoint").forEach { navPoint ->
            val title = navPoint.select("text").firstOrNull()?.text() ?: return@forEach
            val src = navPoint.select("content").firstOrNull()?.attr("src") ?: return@forEach

            val (href, fragmentId) = splitHrefAndFragment(src)
            chapters.add(TOCChapter(title = title, href = href, fragmentId = fragmentId))
        }
    }

    /**
     * Parse EPUB 3 Nav format
     */
    private fun parseNav(content: String, chapters: MutableList<TOCChapter>) {
        val doc = Jsoup.parse(content)

        // Find the toc nav element
        val tocNav = doc.select("nav[epub\\:type=toc], nav#toc").firstOrNull() ?: doc.select("nav").firstOrNull()

        tocNav?.select("a")?.forEach { link ->
            val title = link.text()
            val src = link.attr("href")

            if (title.isNotEmpty() && src.isNotEmpty()) {
                val (href, fragmentId) = splitHrefAndFragment(src)
                chapters.add(TOCChapter(title = title, href = href, fragmentId = fragmentId))
            }
        }
    }

    /**
     * Split href into file path and optional fragment identifier
     */
    private fun splitHrefAndFragment(src: String): Pair<String, String?> {
        val parts = src.split("#", limit = 2)
        return Pair(parts[0], parts.getOrNull(1))
    }

    /**
     * Convert HTML content to plain text for display
     */
    fun htmlToPlainText(html: String): String {
        val doc = Jsoup.parse(html)

        // Remove script and style elements
        doc.select("script, style").remove()

        // Get text with whitespace handling
        doc.outputSettings().prettyPrint(false)

        // Add space after inline elements that might join words
        doc.select("span, em, strong, i, b, a, u, s, sub, sup, small, mark, cite, q, abbr, code").forEach { element ->
            element.append(" ")
        }

        // Replace block elements with line breaks
        doc.select("p, div, br, h1, h2, h3, h4, h5, h6, li, blockquote, pre, hr, section, article, header, footer, aside, main, nav, figure, figcaption, table, tr, td, th, dt, dd").forEach { element ->
            element.prepend("\n\n")
        }

        return doc.text()
            .replace(Regex("[ \\t]+"), " ")  // Collapse horizontal whitespace to single space
            .replace(Regex("\\n{3,}"), "\n\n")  // Collapse multiple newlines
            .replace(Regex(" +\\n"), "\n")  // Remove trailing spaces before newlines
            .replace(Regex("\\n +"), "\n")  // Remove leading spaces after newlines
            .trim()
    }
}
