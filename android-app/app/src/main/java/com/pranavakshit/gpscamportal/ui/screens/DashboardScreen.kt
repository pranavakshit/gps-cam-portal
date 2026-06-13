package com.pranavakshit.gpscamportal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.pranavakshit.gpscamportal.util.UserPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToOfflineGallery: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val role = userPreferences.getRole() ?: "user"
    val isAdmin = role.equals("ADMIN", ignoreCase = true)

    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isAdmin) "Admin Dashboard" else "My Dashboard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = onNavigateToOfflineGallery) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                    }
                    IconButton(onClick = {
                        userPreferences.clear()
                        onLogout()
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            if (isAdmin) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.List, contentDescription = "Photos") },
                        label = { Text("Photos") },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Face, contentDescription = "Users") },
                        label = { Text("Users") },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.LocationOn, contentDescription = "Locations") },
                        label = { Text("Locations") },
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    )
                }
            }
        },
        floatingActionButton = {
            if (!isAdmin || selectedTab == 0) {
                FloatingActionButton(onClick = onNavigateToCamera) {
                    Icon(Icons.Default.Add, contentDescription = "Take Photo")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (isAdmin) {
                when (selectedTab) {
                    0 -> AdminPhotosTab()
                    1 -> AdminUsersTab()
                    2 -> AdminLocationsTab()
                }
            } else {
                UserPhotosTab()
            }
        }
    }
}

@Composable
fun AdminPhotosTab() {
    PhotosFeed(isAdmin = true)
}

@Composable
fun UserPhotosTab() {
    PhotosFeed(isAdmin = false)
}

@Composable
fun PhotosFeed(isAdmin: Boolean) {
    val context = LocalContext.current
    var photos by remember { mutableStateOf<List<com.pranavakshit.gpscamportal.data.remote.PhotoDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        try {
            val apiService = com.pranavakshit.gpscamportal.data.remote.ApiService.create(context)
            val response = apiService.getPhotos()
            if (response.isSuccessful) {
                photos = response.body() ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (photos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(if (isAdmin) "No photos have been uploaded globally yet." else "You haven't uploaded any photos yet.")
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(photos) { photo ->
                Card {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            coil.compose.AsyncImage(
                                model = "https://api.pranavakshit.in" + photo.imageUrl,
                                contentDescription = "Photo",
                                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            if (isAdmin) {
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            try {
                                                val api = com.pranavakshit.gpscamportal.data.remote.ApiService.create(context)
                                                if (api.deletePhoto(photo.id).isSuccessful) {
                                                    photos = photos.filter { it.id != photo.id }
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = photo.locationName, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            Text(text = "By: ${photo.uploader}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminUsersTab() {
    val context = LocalContext.current
    var users by remember { mutableStateOf<List<com.pranavakshit.gpscamportal.data.remote.UserDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    
    val fetchUsers = {
        coroutineScope.launch {
            isLoading = true
            try {
                val apiService = com.pranavakshit.gpscamportal.data.remote.ApiService.create(context)
                val response = apiService.getUsers()
                if (response.isSuccessful) {
                    users = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchUsers()
    }

    if (isLoading && users.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(users) { user ->
                var expanded by remember { mutableStateOf(false) }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = user.username, style = MaterialTheme.typography.titleMedium)
                            Text(text = "Role: ${user.role}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row {
                            Box {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Role")
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Make User") },
                                        onClick = {
                                            expanded = false
                                            coroutineScope.launch {
                                                val api = com.pranavakshit.gpscamportal.data.remote.ApiService.create(context)
                                                if (api.updateUserRole(user.id, com.pranavakshit.gpscamportal.data.remote.UpdateRoleRequest("user")).isSuccessful) {
                                                    fetchUsers()
                                                }
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Make Admin") },
                                        onClick = {
                                            expanded = false
                                            coroutineScope.launch {
                                                val api = com.pranavakshit.gpscamportal.data.remote.ApiService.create(context)
                                                if (api.updateUserRole(user.id, com.pranavakshit.gpscamportal.data.remote.UpdateRoleRequest("ADMIN")).isSuccessful) {
                                                    fetchUsers()
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    val api = com.pranavakshit.gpscamportal.data.remote.ApiService.create(context)
                                    if (api.deleteUser(user.id).isSuccessful) {
                                        fetchUsers()
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}


