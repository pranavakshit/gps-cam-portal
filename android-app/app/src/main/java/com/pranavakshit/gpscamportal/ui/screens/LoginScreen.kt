package com.pranavakshit.gpscamportal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pranavakshit.gpscamportal.data.remote.ApiService
import com.pranavakshit.gpscamportal.data.remote.LoginRequest
import com.pranavakshit.gpscamportal.util.UserPreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val userPreferences = remember { UserPreferences(context) }
    val scope = rememberCoroutineScope()
    
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var totp by remember { mutableStateOf("") }
    var requires2FA by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var show2FAWarningDialog by remember { mutableStateOf(false) }

    // Check if already logged in
    LaunchedEffect(Unit) {
        if (!userPreferences.getToken().isNullOrBlank()) {
            onLoginSuccess()
        }
    }

    val handleLogin = {
        if (username.isBlank() || password.isBlank()) {
            errorMessage = "Please enter both username and password"
        } else {
            isLoading = true
            scope.launch {
                try {
                    val apiService = ApiService.create(context)
                    val trimmedUsername = username.trim()
                    val trimmedPassword = password.trim()
                    val response = apiService.login(LoginRequest(trimmedUsername, trimmedPassword, if (requires2FA) totp else null))
                    
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        userPreferences.saveAuthData(body.token, body.user.username, body.user.role)
                        
                        if (body.user.isTwoFactorEnabled) {
                            onLoginSuccess()
                        } else {
                            onLoginSuccess() // Removed warning dialog
                        }
                    } else if (response.code() == 401) {
                        val errorBodyString = response.errorBody()?.string() ?: ""
                        if (errorBodyString.contains("\"requires2FA\":true") || errorBodyString.contains("\"requires2FA\": true")) {
                            requires2FA = true
                            errorMessage = null
                        } else {
                            errorMessage = "Invalid credentials or token"
                        }
                    } else {
                        errorMessage = "Invalid credentials or network error"
                    }
                } catch (e: Exception) {
                    errorMessage = "Failed to connect to the server"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GPS Cam Portal") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Secure Login",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Please enter your admin-provided credentials.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (!requires2FA) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { 
                        username = it
                        errorMessage = null 
                    },
                    label = { Text("Username") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        errorMessage = null 
                    },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { 
                        focusManager.clearFocus()
                        handleLogin() 
                    }),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = totp,
                    onValueChange = { 
                        val filtered = it.filter { char -> char.isDigit() }
                        if (filtered.length <= 6) {
                            totp = filtered
                            errorMessage = null 
                        }
                    },
                    label = { Text("6-digit Auth Code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { 
                        focusManager.clearFocus()
                        handleLogin() 
                    }),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 16.dp, top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { handleLogin() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (requires2FA) "Verify 2FA" else "Login")
                }
            }
        }

        if (show2FAWarningDialog) {
            AlertDialog(
                onDismissRequest = { 
                    show2FAWarningDialog = false
                    onLoginSuccess()
                },
                title = { Text("Security Warning") },
                text = { Text("Two-Factor Authentication (2FA) is not enabled on your account. Please log in to the web portal to enable it for your security.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            show2FAWarningDialog = false
                            onLoginSuccess()
                        }
                    ) {
                        Text("Continue")
                    }
                }
            )
        }
    }
}
