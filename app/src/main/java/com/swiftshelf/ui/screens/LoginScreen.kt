package com.swiftshelf.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.swiftshelf.R

enum class AuthType(val displayName: String) {
    API_KEY("API Key"),
    USERNAME_PASSWORD("Username / Password")
}

@Composable
fun LoginScreen(
    hostUrl: String,
    apiKey: String,
    username: String,
    password: String,
    authType: AuthType,
    isLoading: Boolean,
    errorMessage: String?,
    onHostUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onAuthTypeChange: (AuthType) -> Unit,
    onConnectClick: () -> Unit
) {
    var showAuthTypeDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(500.dp)
                .padding(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "SwiftShelf",
                    style = MaterialTheme.typography.headlineMedium
                )

                // Auth Type Selector - compact inline version
                var authTypeFocused by remember { mutableStateOf(false) }
                Surface(
                    onClick = { showAuthTypeDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { authTypeFocused = it.isFocused },
                    shape = MaterialTheme.shapes.small,
                    color = if (authTypeFocused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = authType.displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = hostUrl,
                    onValueChange = onHostUrlChange,
                    label = { Text(stringResource(R.string.host_url)) },
                    placeholder = { Text("https://your-server.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !isLoading
                )

                // Show different fields based on auth type
                when (authType) {
                    AuthType.API_KEY -> {
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = onApiKeyChange,
                            label = { Text(stringResource(R.string.api_key)) },
                            placeholder = { Text("Your API Key") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = { if (!isLoading) onConnectClick() }
                            ),
                            enabled = !isLoading
                        )
                    }
                    AuthType.USERNAME_PASSWORD -> {
                        OutlinedTextField(
                            value = username,
                            onValueChange = onUsernameChange,
                            label = { Text("Username") },
                            placeholder = { Text("Your username") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            enabled = !isLoading
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = onPasswordChange,
                            label = { Text("Password") },
                            placeholder = { Text("Your password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = { if (!isLoading) onConnectClick() }
                            ),
                            enabled = !isLoading
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                val canConnect = when (authType) {
                    AuthType.API_KEY -> hostUrl.isNotBlank() && apiKey.isNotBlank()
                    AuthType.USERNAME_PASSWORD -> hostUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
                }

                Button(
                    onClick = onConnectClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !isLoading && canConnect
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.connect), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    // Auth type selection dialog
    if (showAuthTypeDialog) {
        AlertDialog(
            onDismissRequest = { showAuthTypeDialog = false },
            title = { Text("Authentication Method") },
            text = {
                Column {
                    AuthType.entries.forEach { type ->
                        var itemFocused by remember { mutableStateOf(false) }
                        Surface(
                            onClick = {
                                onAuthTypeChange(type)
                                showAuthTypeDialog = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { itemFocused = it.isFocused },
                            color = when {
                                itemFocused -> MaterialTheme.colorScheme.primaryContainer
                                type == authType -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = type.displayName,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        if (type != AuthType.entries.last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAuthTypeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
