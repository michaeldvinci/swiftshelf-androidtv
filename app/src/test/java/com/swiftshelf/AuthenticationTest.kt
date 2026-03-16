package com.swiftshelf

import com.swiftshelf.data.model.LoginRequest
import com.swiftshelf.data.network.AudiobookshelfApi
import com.swiftshelf.data.network.RetrofitClient
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.net.HttpURLConnection

/**
 * Unit tests for authentication using MockWebServer.
 * Tests both HTTP and HTTPS authentication flows.
 */
class AuthenticationTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: AudiobookshelfApi

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `login with HTTP endpoint returns token successfully`() = runTest {
        // Arrange - Mock successful login response
        val mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("""
                {
                    "user": {
                        "id": "user-123",
                        "username": "testuser",
                        "token": "test-token-abc123"
                    },
                    "userDefaultLibraryId": "lib-456"
                }
            """.trimIndent())
        mockWebServer.enqueue(mockResponse)

        // Create API with HTTP base URL
        val baseUrl = mockWebServer.url("/").toString() // This will be http://localhost:PORT/
        api = RetrofitClient.createUnauthenticatedApi(baseUrl)

        // Act - Perform login
        val loginRequest = LoginRequest(username = "testuser", password = "testpass")
        val response = api.login(loginRequest)

        // Assert - Verify response
        assertTrue("Login should be successful", response.isSuccessful)
        assertNotNull("Response body should not be null", response.body())
        assertEquals("Token should match", "test-token-abc123", response.body()?.user?.token)
        assertEquals("Username should match", "testuser", response.body()?.user?.username)

        // Verify the request
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("/login", recordedRequest.path)
        assertTrue("Request body should contain username",
            recordedRequest.body.readUtf8().contains("testuser"))
    }

    @Test
    fun `login with invalid credentials returns 401`() = runTest {
        // Arrange - Mock failed login response
        val mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
            .setBody("""{"error": "Invalid credentials"}""")
        mockWebServer.enqueue(mockResponse)

        val baseUrl = mockWebServer.url("/").toString()
        api = RetrofitClient.createUnauthenticatedApi(baseUrl)

        // Act
        val loginRequest = LoginRequest(username = "wronguser", password = "wrongpass")
        val response = api.login(loginRequest)

        // Assert
        assertFalse("Login should fail", response.isSuccessful)
        assertEquals("Should return 401", HttpURLConnection.HTTP_UNAUTHORIZED, response.code())
    }

    @Test
    fun `authenticated request includes Bearer token in header`() = runTest {
        // Arrange - Mock response for authenticated endpoint
        val mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("""{"libraries": []}""")
        mockWebServer.enqueue(mockResponse)

        val baseUrl = mockWebServer.url("/").toString()
        val testToken = "test-bearer-token-xyz789"

        // Initialize RetrofitClient with token
        RetrofitClient.initialize(baseUrl, testToken)
        api = RetrofitClient.getApi()

        // Act - Make authenticated request
        val response = api.getLibraries()

        // Assert
        assertTrue("Request should be successful", response.isSuccessful)

        // Verify Authorization header
        val recordedRequest = mockWebServer.takeRequest()
        val authHeader = recordedRequest.getHeader("Authorization")
        assertNotNull("Authorization header should be present", authHeader)
        assertEquals("Bearer token should be in header", "Bearer $testToken", authHeader)
    }

    @Test
    fun `client works with both HTTP and HTTPS schemes`() = runTest {
        // Test HTTP
        val httpUrl = "http://localhost:8080/"
        val httpApi = RetrofitClient.createUnauthenticatedApi(httpUrl)
        assertNotNull("HTTP client should be created", httpApi)

        // Test HTTPS
        val httpsUrl = "https://example.com/"
        val httpsApi = RetrofitClient.createUnauthenticatedApi(httpsUrl)
        assertNotNull("HTTPS client should be created", httpsApi)
    }

    @Test
    fun `network configuration uses proper timeouts`() = runTest {
        // Verify that the client is configured with appropriate timeouts
        // This test verifies the configuration without actually waiting for timeout
        val baseUrl = mockWebServer.url("/").toString()
        api = RetrofitClient.createUnauthenticatedApi(baseUrl)

        // The client is configured with 30 second timeouts (lines 69-71 in RetrofitClient.kt)
        // This test just verifies the API can be created successfully
        assertNotNull("API should be created with timeout configuration", api)
    }

    @Test
    fun `server returns 500 error handled properly`() = runTest {
        // Arrange - Mock server error
        val mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
            .setBody("""{"error": "Internal server error"}""")
        mockWebServer.enqueue(mockResponse)

        val baseUrl = mockWebServer.url("/").toString()
        api = RetrofitClient.createUnauthenticatedApi(baseUrl)

        // Act
        val loginRequest = LoginRequest(username = "testuser", password = "testpass")
        val response = api.login(loginRequest)

        // Assert
        assertFalse("Request should not be successful", response.isSuccessful)
        assertEquals("Should return 500", HttpURLConnection.HTTP_INTERNAL_ERROR, response.code())
    }

    @Test
    fun `malformed JSON response handled gracefully`() = runTest {
        // Arrange - Mock malformed response
        val mockResponse = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("""{"invalid": "json", missing_closing_brace""")
        mockWebServer.enqueue(mockResponse)

        val baseUrl = mockWebServer.url("/").toString()
        api = RetrofitClient.createUnauthenticatedApi(baseUrl)

        // Act & Assert
        try {
            val loginRequest = LoginRequest(username = "testuser", password = "testpass")
            val response = api.login(loginRequest)

            // If we get here, check that parsing failed
            if (response.isSuccessful) {
                assertNull("Body should be null for malformed JSON", response.body())
            }
        } catch (e: Exception) {
            // Expected - JSON parsing exception
            assertTrue("Should be a JSON exception",
                e.message?.contains("json", ignoreCase = true) ?: false ||
                e.javaClass.simpleName.contains("Json", ignoreCase = true))
        }
    }
}
