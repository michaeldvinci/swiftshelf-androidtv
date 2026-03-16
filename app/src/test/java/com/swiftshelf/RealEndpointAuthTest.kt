package com.swiftshelf

import com.swiftshelf.data.model.LoginRequest
import com.swiftshelf.data.network.AudiobookshelfApi
import com.swiftshelf.data.network.RetrofitClient
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * Integration tests for authentication against real endpoints.
 *
 * IMPORTANT: These tests are @Ignore'd by default to prevent accidental execution.
 * To run these tests:
 * 1. Remove the @Ignore annotation from the test you want to run
 * 2. Configure the credentials below
 * 3. Run the specific test
 *
 * This allows you to test both HTTP and HTTPS authentication with real servers.
 */
class RealEndpointAuthTest {

    // ===== CONFIGURE VIA ENVIRONMENT VARIABLES =====
    // Set these environment variables when running tests:
    //   TEST_HTTP_URL - e.g., "http://192.168.1.100:13378/"
    //   TEST_HTTPS_URL - e.g., "https://audiobookshelf.example.com/"
    //   TEST_USERNAME - Your username
    //   TEST_PASSWORD - Your password
    //
    // Example:
    //   TEST_HTTP_URL="http://192.168.1.100:13378/" TEST_USERNAME="myuser" TEST_PASSWORD="mypass" ./gradlew testDebugUnitTest --tests "..."
    //
    private val HTTP_BASE_URL = System.getenv("TEST_HTTP_URL")
        ?: "http://localhost:13378/"  // Fallback if not set
    private val HTTP_USERNAME = System.getenv("TEST_USERNAME")
        ?: "your-username-here"
    private val HTTP_PASSWORD = System.getenv("TEST_PASSWORD")
        ?: "your-password-here"

    private val HTTPS_BASE_URL = System.getenv("TEST_HTTPS_URL")
        ?: "https://localhost:13378/"  // Fallback if not set
    private val HTTPS_USERNAME = System.getenv("TEST_USERNAME")
        ?: "your-username-here"  // Same username for both by default
    private val HTTPS_PASSWORD = System.getenv("TEST_PASSWORD")
        ?: "your-password-here"  // Same password for both by default
    // ==================================

    @Before
    fun setup() {
        // Ensure credentials are configured via environment variables
        if (HTTP_USERNAME == "your-username-here" || HTTP_PASSWORD == "your-password-here") {
            println("=" .repeat(80))
            println("WARNING: Test credentials not configured!")
            println("Set environment variables before running tests:")
            println("  TEST_HTTP_URL=\"http://192.168.1.100:13378/\"")
            println("  TEST_HTTPS_URL=\"https://your-server.com/\"")
            println("  TEST_USERNAME=\"youruser\"")
            println("  TEST_PASSWORD=\"yourpass\"")
            println("=" .repeat(80))
        }
    }

    @Test
    @Ignore("Enable manually when testing against real HTTP endpoint")
    fun `login to HTTP endpoint with real credentials`() = runTest {
        // Arrange
        val api = RetrofitClient.createUnauthenticatedApi(HTTP_BASE_URL)
        val loginRequest = LoginRequest(username = HTTP_USERNAME, password = HTTP_PASSWORD)

        // Act
        val response = api.login(loginRequest)

        // Assert
        println("Response code: ${response.code()}")
        println("Response message: ${response.message()}")

        assertTrue("Login should be successful", response.isSuccessful)
        assertNotNull("Response body should not be null", response.body())

        val token = response.body()?.user?.token
        assertNotNull("Token should not be null", token)
        assertFalse("Token should not be empty", token.isNullOrEmpty())

        println("Login successful! Token: ${token?.take(20)}...")
        println("User ID: ${response.body()?.user?.id}")
        println("Username: ${response.body()?.user?.username}")
        println("Default Library ID: ${response.body()?.userDefaultLibraryId}")
    }

    @Test
    @Ignore("Enable manually when testing against real HTTPS endpoint")
    fun `login to HTTPS endpoint with real credentials`() = runTest {
        // Arrange
        val api = RetrofitClient.createUnauthenticatedApi(HTTPS_BASE_URL)
        val loginRequest = LoginRequest(username = HTTPS_USERNAME, password = HTTPS_PASSWORD)

        // Act
        val response = api.login(loginRequest)

        // Assert
        println("Response code: ${response.code()}")
        println("Response message: ${response.message()}")

        assertTrue("Login should be successful", response.isSuccessful)
        assertNotNull("Response body should not be null", response.body())

        val token = response.body()?.user?.token
        assertNotNull("Token should not be null", token)
        assertFalse("Token should not be empty", token.isNullOrEmpty())

        println("Login successful! Token: ${token?.take(20)}...")
        println("User ID: ${response.body()?.user?.id}")
        println("Username: ${response.body()?.user?.username}")
        println("Default Library ID: ${response.body()?.userDefaultLibraryId}")
    }

    @Test
    @Ignore("Enable manually when testing authenticated API calls")
    fun `authenticated API call after login works with HTTP`() = runTest {
        // Step 1: Login
        val loginApi = RetrofitClient.createUnauthenticatedApi(HTTP_BASE_URL)
        val loginRequest = LoginRequest(username = HTTP_USERNAME, password = HTTP_PASSWORD)
        val loginResponse = loginApi.login(loginRequest)

        assertTrue("Login should be successful", loginResponse.isSuccessful)
        val token = loginResponse.body()?.user?.token
        assertNotNull("Token should not be null", token)

        println("Login successful! Token: ${token?.take(20)}...")

        // Step 2: Initialize authenticated client
        RetrofitClient.initialize(HTTP_BASE_URL, token!!)
        val api = RetrofitClient.getApi()

        // Step 3: Make authenticated request
        val librariesResponse = api.getLibraries()

        // Assert
        println("Libraries response code: ${librariesResponse.code()}")
        assertTrue("Get libraries should be successful", librariesResponse.isSuccessful)
        assertNotNull("Libraries response body should not be null", librariesResponse.body())

        val libraries = librariesResponse.body()?.libraries
        println("Found ${libraries?.size ?: 0} libraries")
        libraries?.forEach { library ->
            println("  - ${library.name} (${library.id})")
        }
    }

    @Test
    @Ignore("Enable manually when testing authenticated API calls")
    fun `authenticated API call after login works with HTTPS`() = runTest {
        // Step 1: Login
        val loginApi = RetrofitClient.createUnauthenticatedApi(HTTPS_BASE_URL)
        val loginRequest = LoginRequest(username = HTTPS_USERNAME, password = HTTPS_PASSWORD)
        val loginResponse = loginApi.login(loginRequest)

        assertTrue("Login should be successful", loginResponse.isSuccessful)
        val token = loginResponse.body()?.user?.token
        assertNotNull("Token should not be null", token)

        println("Login successful! Token: ${token?.take(20)}...")

        // Step 2: Initialize authenticated client
        RetrofitClient.initialize(HTTPS_BASE_URL, token!!)
        val api = RetrofitClient.getApi()

        // Step 3: Make authenticated request
        val librariesResponse = api.getLibraries()

        // Assert
        println("Libraries response code: ${librariesResponse.code()}")
        assertTrue("Get libraries should be successful", librariesResponse.isSuccessful)
        assertNotNull("Libraries response body should not be null", librariesResponse.body())

        val libraries = librariesResponse.body()?.libraries
        println("Found ${libraries?.size ?: 0} libraries")
        libraries?.forEach { library ->
            println("  - ${library.name} (${library.id})")
        }
    }

    @Test
    @Ignore("Enable manually to test invalid credentials")
    fun `login with invalid credentials fails gracefully with HTTP`() = runTest {
        val api = RetrofitClient.createUnauthenticatedApi(HTTP_BASE_URL)
        val loginRequest = LoginRequest(username = "invalid_user", password = "wrong_password")

        val response = api.login(loginRequest)

        println("Response code: ${response.code()}")
        println("Response message: ${response.message()}")

        assertFalse("Login should fail with invalid credentials", response.isSuccessful)
        assertEquals("Should return 401 Unauthorized", 401, response.code())
    }

    @Test
    @Ignore("Enable manually to test invalid credentials")
    fun `login with invalid credentials fails gracefully with HTTPS`() = runTest {
        val api = RetrofitClient.createUnauthenticatedApi(HTTPS_BASE_URL)
        val loginRequest = LoginRequest(username = "invalid_user", password = "wrong_password")

        val response = api.login(loginRequest)

        println("Response code: ${response.code()}")
        println("Response message: ${response.message()}")

        assertFalse("Login should fail with invalid credentials", response.isSuccessful)
        assertEquals("Should return 401 Unauthorized", 401, response.code())
    }

    @Test
    @Ignore("Enable manually to test protocol switching")
    fun `client can switch between HTTP and HTTPS`() = runTest {
        // Test HTTP
        println("Testing HTTP endpoint...")
        val httpApi = RetrofitClient.createUnauthenticatedApi(HTTP_BASE_URL)
        val httpLogin = LoginRequest(username = HTTP_USERNAME, password = HTTP_PASSWORD)
        val httpResponse = httpApi.login(httpLogin)

        println("HTTP Response: ${httpResponse.code()}")
        assertTrue("HTTP login should work", httpResponse.isSuccessful)

        // Test HTTPS
        println("\nTesting HTTPS endpoint...")
        val httpsApi = RetrofitClient.createUnauthenticatedApi(HTTPS_BASE_URL)
        val httpsLogin = LoginRequest(username = HTTPS_USERNAME, password = HTTPS_PASSWORD)
        val httpsResponse = httpsApi.login(httpsLogin)

        println("HTTPS Response: ${httpsResponse.code()}")
        assertTrue("HTTPS login should work", httpsResponse.isSuccessful)

        println("\nBoth HTTP and HTTPS work correctly!")
    }
}
