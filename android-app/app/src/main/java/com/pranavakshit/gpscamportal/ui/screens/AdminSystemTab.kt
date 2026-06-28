package com.pranavakshit.gpscamportal.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pranavakshit.gpscamportal.data.remote.ApiService
import com.pranavakshit.gpscamportal.data.remote.DockerStats
import kotlinx.coroutines.launch

@Composable
fun AdminSystemTab() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<DockerStats?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isPruning by remember { mutableStateOf(false) }

    val fetchStats = {
        coroutineScope.launch {
            isLoading = true
            try {
                val api = ApiService.create(context)
                val response = api.getDockerStats()
                if (response.isSuccessful) {
                    stats = response.body()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchStats()
    }

    if (isLoading && stats == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (stats != null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("System Management", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp))

            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Docker Resource Usage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Memory Usage: ${String.format("%.2f", stats!!.memoryUsageGB)} GB / ${String.format("%.2f", stats!!.memoryLimitGB)} GB")
                    LinearProgressIndicator(
                        progress = { (stats!!.memoryPercentage / 100).toFloat() },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                    
                    Text("CPU Usage: ${String.format("%.2f", stats!!.cpuPercentage)}%")
                    LinearProgressIndicator(
                        progress = { (stats!!.cpuPercentage / 100).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                    
                    Text("Running Containers: ${stats!!.totalContainers}")
                }
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        isPruning = true
                        try {
                            val api = ApiService.create(context)
                            val response = api.pruneDocker()
                            if (response.isSuccessful) {
                                Toast.makeText(context, "Docker system pruned successfully", Toast.LENGTH_SHORT).show()
                                fetchStats()
                            } else {
                                Toast.makeText(context, "Failed to prune docker", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error pruning docker", Toast.LENGTH_SHORT).show()
                        } finally {
                            isPruning = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                enabled = !isPruning
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Prune")
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isPruning) "Pruning..." else "Prune System (Free Space)")
            }

            Text("Active Containers", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))

            LazyColumn {
                items(stats!!.containers) { container ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = "Container")
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(container.name, fontWeight = FontWeight.Bold)
                                Text("Status: ${container.status} | Size: ${container.size}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Failed to load system stats.")
        }
    }
}
