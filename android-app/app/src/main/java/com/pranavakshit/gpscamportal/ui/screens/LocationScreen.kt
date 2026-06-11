package com.pranavakshit.gpscamportal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    onLocationSelected: (String, String, String) -> Unit
) {
    var state by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Location") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Where are you capturing photos?",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = state,
                onValueChange = { state = it; showError = false },
                label = { Text("State") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = district,
                onValueChange = { district = it; showError = false },
                label = { Text("District") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = area,
                onValueChange = { area = it; showError = false },
                label = { Text("Area / Location Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (showError) {
                Text(
                    text = "Please fill in all location fields",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (state.isNotBlank() && district.isNotBlank() && area.isNotBlank()) {
                        onLocationSelected(state.trim(), district.trim(), area.trim())
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Start Camera")
            }
        }
    }
}
