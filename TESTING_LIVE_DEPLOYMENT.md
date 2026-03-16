# Testing Against Live Deployment

This guide shows you how to test your app's authentication against your actual HTTP/HTTPS servers using **environment variables** (no credentials in code!).

## Quick Start

### 1. Set Environment Variables

The tests use environment variables to keep credentials secure and out of version control:

```bash
export TEST_HTTP_URL="http://192.168.1.100:13378/"
export TEST_HTTPS_URL="https://abs.example.com/"
export TEST_USERNAME="youruser"
export TEST_PASSWORD="yourpassword"
```

**Environment Variables:**
- `TEST_HTTP_URL` - Your HTTP server URL (e.g., local dev server)
- `TEST_HTTPS_URL` - Your HTTPS server URL (e.g., production server)
- `TEST_USERNAME` - Your username (used for both HTTP and HTTPS)
- `TEST_PASSWORD` - Your password (used for both HTTP and HTTPS)

**Important Notes:**
- Make sure URLs end with `/` (e.g., `http://192.168.1.100:13378/`)
- Use the full URL including protocol (`http://` or `https://`)
- For AudioBookshelf servers, the default port is usually 13378
- If not set, tests will use placeholder values and warn you

### 2. Enable the Test You Want to Run

Find the test in `RealEndpointAuthTest.kt` and **comment out or delete** the `@Ignore` line:

**Before:**
```kotlin
@Test
@Ignore("Enable manually when testing against real HTTP endpoint")  // ← Remove this line
fun `login to HTTP endpoint with real credentials`() = runTest {
```

**After:**
```kotlin
@Test
// @Ignore("Enable manually when testing against real HTTP endpoint")  // ← Commented out
fun `login to HTTP endpoint with real credentials`() = runTest {
```

### 3. Run the Test

#### Option A: Using Gradle (Command Line) - Inline Variables

Pass environment variables directly with the command:

```bash
# Test HTTP endpoint
TEST_HTTP_URL="http://192.168.1.100:13378/" \
TEST_USERNAME="myuser" \
TEST_PASSWORD="mypass" \
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
./gradlew testDebugUnitTest --tests "com.swiftshelf.RealEndpointAuthTest.login to HTTP endpoint with real credentials"

# Test HTTPS endpoint
TEST_HTTPS_URL="https://abs.example.com/" \
TEST_USERNAME="myuser" \
TEST_PASSWORD="mypass" \
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
./gradlew testDebugUnitTest --tests "com.swiftshelf.RealEndpointAuthTest.login to HTTPS endpoint with real credentials"
```

#### Option B: Using Exported Variables

Set variables once, then run multiple tests:

```bash
# Set variables
export TEST_HTTP_URL="http://192.168.1.100:13378/"
export TEST_HTTPS_URL="https://abs.example.com/"
export TEST_USERNAME="myuser"
export TEST_PASSWORD="mypass"
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home

# Run tests
./gradlew testDebugUnitTest --tests "com.swiftshelf.RealEndpointAuthTest.login to HTTP endpoint with real credentials"
./gradlew testDebugUnitTest --tests "com.swiftshelf.RealEndpointAuthTest.authenticated API call after login works with HTTP"
```

#### Option C: Using Android Studio

1. Open Run → Edit Configurations
2. Select the test configuration
3. Add environment variables in "Environment variables" field:
   ```
   TEST_HTTP_URL=http://192.168.1.100:13378/;TEST_USERNAME=myuser;TEST_PASSWORD=mypass
   ```
4. Click the green ▶ arrow next to the test method
5. View the output in the "Run" window at the bottom

### 4. Check the Results

If the test **passes**, you'll see output like:
```
Response code: 200
Response message: OK
Login successful! Token: test-token-abc123de...
User ID: user-abc123
Username: youruser
Default Library ID: lib-456

BUILD SUCCESSFUL
```

If the test **fails**, check:
- Is your server running and accessible?
- Are the credentials correct?
- Is the URL correct (including protocol and trailing `/`)?
- Can you access the URL in a browser?

## Available Tests

### Basic Login Tests

1. **`login to HTTP endpoint with real credentials`** (line 46)
   - Tests basic HTTP login
   - Returns user info and token

2. **`login to HTTPS endpoint with real credentials`** (line 73)
   - Tests basic HTTPS login
   - Returns user info and token

### Full Flow Tests

3. **`authenticated API call after login works with HTTP`** (line 100)
   - Logs in via HTTP
   - Uses the token to fetch libraries
   - Verifies the full authentication flow

4. **`authenticated API call after login works with HTTPS`** (line 125)
   - Logs in via HTTPS
   - Uses the token to fetch libraries
   - Verifies the full authentication flow

### Error Tests

5. **`login with invalid credentials fails gracefully with HTTP`** (line 150)
   - Tests that wrong credentials return 401
   - Verifies error handling

6. **`login with invalid credentials fails gracefully with HTTPS`** (line 161)
   - Tests that wrong credentials return 401 on HTTPS
   - Verifies error handling

### Protocol Switching Test

7. **`client can switch between HTTP and HTTPS`** (line 172)
   - Tests both HTTP and HTTPS in one test
   - Verifies the app can handle protocol switching

## Example: Test Your Local Server

Let's say you have AudioBookshelf running locally at `http://192.168.1.100:13378`:

### Step 1: Set Environment Variables
```bash
export TEST_HTTP_URL="http://192.168.1.100:13378/"
export TEST_USERNAME="admin"
export TEST_PASSWORD="mypassword"
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
```

### Step 2: Enable Test
In `RealEndpointAuthTest.kt`, find line 66 and comment out the `@Ignore`:
```kotlin
@Test
// @Ignore("Enable manually when testing against real HTTP endpoint")
fun `login to HTTP endpoint with real credentials`() = runTest {
```

### Step 3: Run
```bash
./gradlew testDebugUnitTest --tests "com.swiftshelf.RealEndpointAuthTest.login to HTTP endpoint with real credentials"
```

Or use inline variables (one-liner):
```bash
TEST_HTTP_URL="http://192.168.1.100:13378/" TEST_USERNAME="admin" TEST_PASSWORD="mypassword" JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew testDebugUnitTest --tests "com.swiftshelf.RealEndpointAuthTest.login to HTTP endpoint with real credentials"
```

### Step 4: See Results
```
Response code: 200
Response message: OK
Login successful! Token: abc123xyz789...
User ID: user-abc
Username: admin
Default Library ID: lib-main

✅ Test passed!
```

## Security Tips

1. **Environment variables keep credentials safe** ✅
   - Credentials are never in the code
   - Safe to commit `RealEndpointAuthTest.kt` to git
   - Each developer uses their own credentials
   - CI/CD can use different credentials via environment

2. **Use a test account**
   - Don't use your main admin account for testing
   - Create a dedicated test user with limited permissions

3. **Shell history considerations**
   - Environment variables appear in shell history
   - Clear sensitive commands: `history -c` (bash) or `clear` (zsh)
   - Or prefix with a space: ` export TEST_PASSWORD="secret"` (if HISTCONTROL=ignorespace)

4. **Use a `.env` file for convenience** (optional)
   Create a `.env` file (add to `.gitignore`):
   ```bash
   # .env
   export TEST_HTTP_URL="http://192.168.1.100:13378/"
   export TEST_USERNAME="admin"
   export TEST_PASSWORD="mypassword"
   ```

   Then source it:
   ```bash
   source .env
   ./gradlew testDebugUnitTest --tests "..."
   ```

## Troubleshooting

### "Connection refused"
- Server is not running
- Wrong IP address or port
- Firewall blocking the connection

### "401 Unauthorized"
- Wrong username or password
- Check credentials in AudioBookshelf web UI

### "Certificate error" (HTTPS)
- Self-signed certificate
- You may need to configure OkHttp to trust your cert (for dev only!)

### "Unknown host"
- Check the domain name
- Try using IP address instead
- Check DNS resolution

### "Read timeout"
- Server is too slow to respond
- Check server health
- Increase timeout in RetrofitClient.kt (currently 30 seconds)

## What This Tests

These tests verify:
- ✅ Your credentials work with the server
- ✅ The server returns a valid token
- ✅ The token can be used for authenticated API calls
- ✅ Both HTTP and HTTPS protocols work
- ✅ Error handling (wrong credentials, server errors)
- ✅ The app can switch between HTTP and HTTPS

This gives you confidence that your authentication implementation works with real servers!
