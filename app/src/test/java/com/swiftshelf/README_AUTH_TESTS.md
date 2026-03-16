# Authentication Testing Guide

This directory contains comprehensive authentication tests for both HTTP and HTTPS endpoints.

## Test Files

### 1. `AuthenticationTest.kt` - Unit Tests with Mocked Server
Uses MockWebServer to test authentication logic without requiring a real server.

**What it tests:**
- HTTP login flow
- HTTPS login flow
- Bearer token authentication
- Invalid credentials handling
- Timeout scenarios
- Server errors (500, 401, etc.)
- Malformed response handling

**How to run:**
```bash
./gradlew test --tests "com.swiftshelf.AuthenticationTest"
```

These tests run automatically and don't require any configuration.

### 2. `RealEndpointAuthTest.kt` - Integration Tests with Real Servers
Tests authentication against actual HTTP/HTTPS endpoints using **environment variables** (secure, no credentials in code).

**What it tests:**
- Real HTTP endpoint login
- Real HTTPS endpoint login
- Authenticated API calls after login
- Invalid credentials with real server
- Protocol switching between HTTP and HTTPS

**How to run:**

1. **Set environment variables** with your credentials:
   ```bash
   export TEST_HTTP_URL="http://192.168.1.100:13378/"
   export TEST_HTTPS_URL="https://audiobookshelf.example.com/"
   export TEST_USERNAME="myuser"
   export TEST_PASSWORD="mypass123"
   ```

   Or use the `.env.example` template:
   ```bash
   cp .env.example .env
   # Edit .env with your actual values
   source .env
   ```

2. **Remove the `@Ignore` annotation** from the test you want to run:
   ```kotlin
   @Test
   // @Ignore("Enable manually when testing...")  // <-- Comment this out
   fun `login to HTTP endpoint with real credentials`() = runTest {
       // ...
   }
   ```

3. **Run the specific test:**
   ```bash
   # Run a specific test (with env vars already exported)
   ./gradlew test --tests "com.swiftshelf.RealEndpointAuthTest.login to HTTP endpoint with real credentials"

   # Or pass env vars inline
   TEST_HTTP_URL="http://192.168.1.100:13378/" TEST_USERNAME="myuser" TEST_PASSWORD="mypass" ./gradlew test --tests "com.swiftshelf.RealEndpointAuthTest.login to HTTP endpoint with real credentials"
   ```

## Common Scenarios

### Testing HTTP Server
1. Set environment variables:
   ```bash
   export TEST_HTTP_URL="http://192.168.1.100:13378/"
   export TEST_USERNAME="myuser"
   export TEST_PASSWORD="mypass"
   ```
2. Remove `@Ignore` from `login to HTTP endpoint with real credentials`
3. Run the test

### Testing HTTPS Server
1. Set environment variables:
   ```bash
   export TEST_HTTPS_URL="https://abs.example.com/"
   export TEST_USERNAME="myuser"
   export TEST_PASSWORD="mypass"
   ```
2. Remove `@Ignore` from `login to HTTPS endpoint with real credentials`
3. Run the test

### Testing Both Protocols
1. Set all environment variables:
   ```bash
   export TEST_HTTP_URL="http://192.168.1.100:13378/"
   export TEST_HTTPS_URL="https://abs.example.com/"
   export TEST_USERNAME="myuser"
   export TEST_PASSWORD="mypass"
   ```
2. Remove `@Ignore` from `client can switch between HTTP and HTTPS`
3. Run the test - it will verify both protocols work correctly

### Testing Authenticated API Calls
1. Set environment variables for your chosen protocol
2. Remove `@Ignore` from the appropriate authenticated test
3. Run the test - it will login and then fetch libraries to verify the token works

## Gradle Commands

```bash
# Run all unit tests (mocked tests only)
./gradlew test

# Run only AuthenticationTest (mocked)
./gradlew test --tests "com.swiftshelf.AuthenticationTest"

# Run a specific test method
./gradlew test --tests "com.swiftshelf.AuthenticationTest.login with HTTP endpoint returns token successfully"

# Run tests with detailed output
./gradlew test --info

# Run tests and see print statements
./gradlew test --info | grep -A 10 "Response code"
```

## Android Studio

You can also run tests directly in Android Studio:
1. Open the test file
2. Click the green arrow next to the test class or method
3. View results in the Run window
4. Check "Run/Debug Configurations" if you need to modify test settings

## Troubleshooting

### "RetrofitClient not initialized" Error
This happens when testing authenticated endpoints without initializing the client. The real endpoint tests handle this automatically.

### Certificate/SSL Errors with HTTPS
If you're using self-signed certificates in development, you may need to configure OkHttp to trust your certificate. This is NOT recommended for production.

### Connection Refused
- Verify your server is running
- Check the base URL is correct (include trailing slash: `http://server:port/`)
- Ensure your device/emulator can reach the server
- For localhost, use `10.0.2.2` instead of `localhost` when running on Android Emulator

### Test Timeout
The client has a 30-second timeout. If your server is slow to respond, tests may fail. Check the server logs.

## Best Practices

1. **Never commit real credentials** - Keep test credentials out of version control
2. **Use environment variables** - For CI/CD, consider using environment variables:
   ```kotlin
   private val HTTP_USERNAME = System.getenv("TEST_USERNAME") ?: "your-username"
   ```
3. **Keep @Ignore on by default** - Only remove when actively testing to prevent accidental execution
4. **Test both protocols** - Ensure your app works with both HTTP (local dev) and HTTPS (production)

## Security Notes

- HTTP sends credentials in plain text - only use for local development
- HTTPS encrypts the connection - required for production
- Bearer tokens in the `Authorization` header work with both protocols
- The app uses the standard `Authorization: Bearer <token>` format
