package com.swiftshelf.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swiftshelf.data.model.LibraryFile
import com.swiftshelf.data.model.LibraryItem
import com.swiftshelf.epub.EPUBParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@Composable
fun EpubReaderScreen(
    item: LibraryItem,
    ebookFile: LibraryFile,
    hostUrl: String,
    apiToken: String,
    onDismiss: () -> Unit
) {
    // Sepia color scheme
    val sepiaBackground = Color(0xFFF2E8D2)
    val sepiaText = Color(0xFF45382C)

    // State
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var paginatedPages by remember { mutableStateOf<List<String>>(emptyList()) }
    var tocChapters by remember { mutableStateOf<List<EPUBParser.TOCChapter>>(emptyList()) }
    var spineToPageMap by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var currentPage by remember { mutableIntStateOf(0) }
    var showChapterMenu by remember { mutableStateOf(false) }

    // Handle back button properly on Android TV
    BackHandler(enabled = true) {
        if (showChapterMenu) {
            showChapterMenu = false
        } else {
            onDismiss()
        }
    }

    // Page dimensions for pagination
    val pageHeight = 800f
    val lineHeight = 30f
    val charsPerLine = 70

    // Load EPUB on launch
    LaunchedEffect(item.id, ebookFile.ino) {
        try {
            isLoading = true
            errorMessage = null

            val ebookData = withContext(Dispatchers.IO) {
                downloadEbook(hostUrl, item.id, ebookFile.ino ?: "", apiToken)
            }

            if (ebookData != null) {
                val epubContent = withContext(Dispatchers.Default) {
                    EPUBParser.parse(ebookData)
                }

                tocChapters = epubContent.tocChapters

                // Paginate content
                val (pages, spineMap) = withContext(Dispatchers.Default) {
                    paginateContent(epubContent.spineItems, pageHeight, lineHeight, charsPerLine)
                }

                paginatedPages = pages
                spineToPageMap = spineMap
            } else {
                errorMessage = "Failed to download ebook"
            }

            isLoading = false
        } catch (e: Exception) {
            errorMessage = "Error loading ebook: ${e.message}"
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(sepiaBackground)
    ) {
        when {
            isLoading -> {
                // Loading state
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    CircularProgressIndicator(color = sepiaText)
                    Text(
                        text = "Loading ebook...",
                        color = sepiaText,
                        fontSize = 18.sp
                    )
                }
            }

            errorMessage != null -> {
                // Error state
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Error Loading Ebook",
                        color = sepiaText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = errorMessage ?: "",
                        color = sepiaText.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = sepiaText,
                            contentColor = sepiaBackground
                        )
                    ) {
                        Text("Go Back")
                    }
                }
            }

            else -> {
                // Reader content
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Two-panel layout
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 40.dp, vertical = 20.dp)
                    ) {
                        // Left page (even page numbers - currentPage - 1)
                        PagePanel(
                            pageNumber = currentPage - 1,
                            pages = paginatedPages,
                            item = item,
                            sepiaText = sepiaText,
                            pageHeight = pageHeight,
                            modifier = Modifier.weight(1f)
                        )

                        // Divider
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(sepiaText.copy(alpha = 0.2f))
                        )

                        // Right page (odd page numbers - currentPage)
                        PagePanel(
                            pageNumber = currentPage,
                            pages = paginatedPages,
                            item = item,
                            sepiaText = sepiaText,
                            pageHeight = pageHeight,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Navigation buttons at bottom
                    NavigationBar(
                        currentPage = currentPage,
                        totalPages = paginatedPages.size,
                        sepiaText = sepiaText,
                        sepiaBackground = sepiaBackground,
                        onPreviousPage = {
                            if (currentPage > 1) {
                                currentPage -= 2
                            }
                        },
                        onNextPage = {
                            if (currentPage < paginatedPages.size - 1) {
                                currentPage += 2
                            }
                        },
                        onShowChapters = { showChapterMenu = true }
                    )
                }

                // Chapter menu overlay
                AnimatedVisibility(
                    visible = showChapterMenu,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    ChapterMenuOverlay(
                        tocChapters = tocChapters,
                        spineToPageMap = spineToPageMap,
                        sepiaText = sepiaText,
                        sepiaBackground = sepiaBackground,
                        onChapterSelected = { tocChapter ->
                            spineToPageMap[tocChapter.href]?.let { startPage ->
                                currentPage = startPage + 1
                            }
                            showChapterMenu = false
                        },
                        onDismiss = { showChapterMenu = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun PagePanel(
    pageNumber: Int,
    pages: List<String>,
    item: LibraryItem,
    sepiaText: Color,
    pageHeight: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxHeight()) {
        when {
            pageNumber < 0 -> {
                // Cover/title page
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = item.media?.metadata?.title ?: "Unknown",
                        color = sepiaText,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    item.media?.metadata?.authors?.firstOrNull()?.name?.let { author ->
                        Text(
                            text = author,
                            color = sepiaText.copy(alpha = 0.7f),
                            fontSize = 24.sp
                        )
                    }
                }
            }

            pageNumber < pages.size -> {
                // Page content
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = pages[pageNumber],
                        color = sepiaText,
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 30.dp, vertical = 20.dp)
                            .verticalScroll(rememberScrollState())
                    )

                    // Page number
                    Text(
                        text = "${pageNumber + 1}",
                        color = sepiaText.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 20.dp)
                    )
                }
            }

            else -> {
                // Empty page beyond content
                Spacer(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun NavigationBar(
    currentPage: Int,
    totalPages: Int,
    sepiaText: Color,
    sepiaBackground: Color,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onShowChapters: () -> Unit
) {
    var leftFocused by remember { mutableStateOf(false) }
    var rightFocused by remember { mutableStateOf(false) }
    var chaptersFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left arrow button
        IconButton(
            onClick = onPreviousPage,
            enabled = currentPage > 1,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (leftFocused) sepiaText else Color.Transparent)
                .onFocusChanged { leftFocused = it.isFocused }
                .focusable()
        ) {
            Icon(
                Icons.Default.ChevronLeft,
                contentDescription = "Previous page",
                tint = if (leftFocused) sepiaBackground else sepiaText.copy(alpha = if (currentPage > 1) 0.5f else 0.2f),
                modifier = Modifier.size(30.dp)
            )
        }

        // Chapters button
        Button(
            onClick = onShowChapters,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (chaptersFocused) sepiaText else sepiaText.copy(alpha = 0.1f),
                contentColor = if (chaptersFocused) sepiaBackground else sepiaText.copy(alpha = 0.7f)
            ),
            modifier = Modifier
                .onFocusChanged { chaptersFocused = it.isFocused }
                .focusable()
        ) {
            Icon(
                Icons.Default.List,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Chapters")
        }

        // Right arrow button
        IconButton(
            onClick = onNextPage,
            enabled = currentPage < totalPages - 1,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (rightFocused) sepiaText else Color.Transparent)
                .onFocusChanged { rightFocused = it.isFocused }
                .focusable()
        ) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Next page",
                tint = if (rightFocused) sepiaBackground else sepiaText.copy(alpha = if (currentPage < totalPages - 1) 0.5f else 0.2f),
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun ChapterMenuOverlay(
    tocChapters: List<EPUBParser.TOCChapter>,
    spineToPageMap: Map<String, Int>,
    sepiaText: Color,
    sepiaBackground: Color,
    onChapterSelected: (EPUBParser.TOCChapter) -> Unit,
    onDismiss: () -> Unit
) {
    val firstChapterFocusRequester = remember { FocusRequester() }

    // Request focus on first chapter when menu opens
    LaunchedEffect(Unit) {
        if (tocChapters.isNotEmpty()) {
            kotlinx.coroutines.delay(100)
            try {
                firstChapterFocusRequester.requestFocus()
            } catch (e: Exception) {
                // Focus request might fail if not yet attached
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(600.dp)
                .heightIn(max = 600.dp)
                .clickable(enabled = false) { }, // Prevent click-through
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = sepiaBackground)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chapters",
                        color = sepiaText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = sepiaText
                        )
                    }
                }

                Divider(color = sepiaText.copy(alpha = 0.2f))

                // Chapter list
                if (tocChapters.isEmpty()) {
                    Text(
                        text = "No chapters found",
                        color = sepiaText.copy(alpha = 0.5f),
                        modifier = Modifier.padding(24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(tocChapters) { index, chapter ->
                            ChapterRow(
                                chapter = chapter,
                                pageNumber = spineToPageMap[chapter.href],
                                sepiaText = sepiaText,
                                isFirst = index == 0,
                                firstFocusRequester = if (index == 0) firstChapterFocusRequester else null,
                                onClick = { onChapterSelected(chapter) }
                            )

                            if (index < tocChapters.size - 1) {
                                Divider(color = sepiaText.copy(alpha = 0.1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterRow(
    chapter: EPUBParser.TOCChapter,
    pageNumber: Int?,
    sepiaText: Color,
    isFirst: Boolean = false,
    firstFocusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) sepiaText else Color.Transparent)
            .clickable(onClick = onClick)
            .onFocusChanged { isFocused = it.isFocused }
            .then(if (firstFocusRequester != null) Modifier.focusRequester(firstFocusRequester) else Modifier)
            .focusable()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = chapter.title,
            color = if (isFocused) Color(0xFFF2E8D2) else sepiaText,
            fontSize = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        pageNumber?.let {
            Text(
                text = "Page ${it + 1}",
                color = if (isFocused) Color(0xFFF2E8D2).copy(alpha = 0.7f) else sepiaText.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Download ebook from server
 */
private suspend fun downloadEbook(
    hostUrl: String,
    itemId: String,
    ebookIno: String,
    apiToken: String
): ByteArray? = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("$hostUrl/api/items/$itemId/ebook/$ebookIno")
            .addHeader("Authorization", "Bearer $apiToken")
            .build()

        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            response.body?.bytes()
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Paginate content into pages that fit the screen
 */
private fun paginateContent(
    spineItems: List<EPUBParser.SpineItem>,
    pageHeight: Float,
    lineHeight: Float,
    charsPerLine: Int
): Pair<List<String>, Map<String, Int>> {
    val pages = mutableListOf<String>()
    val spineMap = mutableMapOf<String, Int>()
    val linesPerPage = (pageHeight / lineHeight).toInt()

    var currentPageLines = mutableListOf<String>()
    var currentLineCount = 0

    // Helper to commit current line
    fun commitLine(line: String) {
        currentPageLines.add(line)
        currentLineCount++

        if (currentLineCount >= linesPerPage) {
            pages.add(currentPageLines.joinToString("\n"))
            currentPageLines = mutableListOf()
            currentLineCount = 0
        }
    }

    // Helper to handle words that are longer than a line
    fun addWordWithWrap(word: String) {
        if (word.length <= charsPerLine) {
            commitLine(word)
        } else {
            // Break long word across multiple lines
            var remaining = word
            while (remaining.isNotEmpty()) {
                val chunk = remaining.take(charsPerLine)
                remaining = remaining.drop(charsPerLine)
                commitLine(chunk)
            }
        }
    }

    for (spineItem in spineItems) {
        // Mark the start page of this spine item (don't force page break)
        spineMap[spineItem.href] = pages.size

        val plainText = EPUBParser.htmlToPlainText(spineItem.htmlContent)
        val paragraphs = plainText.split("\n\n")

        for (paragraph in paragraphs) {
            val trimmedParagraph = paragraph.trim()
            if (trimmedParagraph.isEmpty()) continue

            // Split paragraph into words, preserving all content
            val words = trimmedParagraph.split(Regex("\\s+")).filter { it.isNotEmpty() }
            var currentLine = ""

            for (word in words) {
                if (currentLine.isEmpty()) {
                    // Starting a new line
                    if (word.length > charsPerLine) {
                        // Word is too long, need to break it
                        addWordWithWrap(word)
                        currentLine = ""
                    } else {
                        currentLine = word
                    }
                } else {
                    val testLine = "$currentLine $word"

                    if (testLine.length <= charsPerLine) {
                        // Word fits on current line
                        currentLine = testLine
                    } else {
                        // Word doesn't fit, commit current line first
                        commitLine(currentLine)

                        // Handle the word that didn't fit
                        if (word.length > charsPerLine) {
                            addWordWithWrap(word)
                            currentLine = ""
                        } else {
                            currentLine = word
                        }
                    }
                }
            }

            // Add remaining line from paragraph (don't lose it!)
            if (currentLine.isNotEmpty()) {
                commitLine(currentLine)
            }

            // Add paragraph break (blank line) only if we have room
            if (currentLineCount < linesPerPage - 1) {
                commitLine("")
            }
        }
    }

    // Add any remaining content as final page
    if (currentPageLines.isNotEmpty()) {
        pages.add(currentPageLines.joinToString("\n"))
    }

    return Pair(pages, spineMap)
}
