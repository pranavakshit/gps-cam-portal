package com.pranavakshit.gpscamportal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pranavakshit.gpscamportal.util.UserPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    
    var uploaderName by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    // Check if already logged in
    LaunchedEffect(Unit) {
        if (!userPreferences.getUploaderName().isNullOrBlank()) {
            onLoginSuccess()
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
                text = "Offline Uploader Setup",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Enter your name or ID to tag the photos you capture offline.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = uploaderName,
                onValueChange = { 
                    uploaderName = it
                    showError = false 
                },
                label = { Text("Uploader Name / ID") },
                isError = showError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (showError) {
                Text(
                    text = "Please enter a valid name",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 16.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (uploaderName.isNotBlank()) {
                        userPreferences.saveUploaderName(uploaderName.trim())
                        onLoginSuccess()
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Continue Offline")
            }
        }
    }
}
