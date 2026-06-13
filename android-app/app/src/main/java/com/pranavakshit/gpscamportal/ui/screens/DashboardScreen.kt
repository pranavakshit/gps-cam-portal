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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pranavakshit.gpscamportal.data.remote.PhotoDto

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotosFeed(isAdmin: Boolean) {
    val context = LocalContext.current
    var photos by remember { mutableStateOf<List<com.pranavakshit.gpscamportal.data.remote.PhotoDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    
    // Selection state
    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Int>() }
    
    // Viewer state
    var viewingPhoto by remember { mutableStateOf<com.pranavakshit.gpscamportal.data.remote.PhotoDto?>(null) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    
    // Dialog states to match web
    var photoToRequestDelete by remember { mutableStateOf<Int?>(null) }
    var deleteReason by remember { mutableStateOf("") }
    var photoToConfirmAction by remember { mutableStateOf<Pair<Int, String>?>(null) } // action can be "delete" or "hard_delete" (not used in dashboard yet but good for consistency)
    
    val fetchPhotos = {
        coroutineScope.launch {
            isLoading = true
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
    }

    LaunchedEffect(Unit) {
        fetchPhotos()
    }

    androidx.activity.compose.BackHandler(enabled = isMultiSelectMode) {
        isMultiSelectMode = false
        selectedIds.clear()
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
        Column(modifier = Modifier.fillMaxSize()) {
            if (isMultiSelectMode) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${selectedIds.size} Selected")
                    Row {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                val api = com.pranavakshit.gpscamportal.data.remote.ApiService.create(context)
                                selectedIds.forEach { id ->
                                    if (isAdmin) {
                                        api.deletePhoto(id)
                                    } else {
                                        val req = com.pranavakshit.gpscamportal.data.remote.DeletionRequest(reason = "Bulk Deletion Request")
                                        api.requestDeletePhoto(id, req)
                                    }
                                }
                                selectedIds.clear()
                                isMultiSelectMode = false
                                android.widget.Toast.makeText(context, "Bulk action processed", android.widget.Toast.LENGTH_SHORT).show()
                                fetchPhotos()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                        }
                        IconButton(onClick = { isMultiSelectMode = false; selectedIds.clear() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    }
                }
            }
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(photos) { photo ->
                    val isSelected = selectedIds.contains(photo.id)
                    Card(modifier = Modifier.combinedClickable(
                        onClick = {
                            if (isMultiSelectMode) {
                                if (isSelected) selectedIds.remove(photo.id) else selectedIds.add(photo.id)
                                if (selectedIds.isEmpty()) isMultiSelectMode = false
                            } else {
                                viewingPhoto = photo
                            }
                        },
                        onLongClick = {
                            isMultiSelectMode = true
                            if (isSelected) selectedIds.remove(photo.id) else selectedIds.add(photo.id)
                        }
                    )) {
                        Column {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                coil.compose.AsyncImage(
                                    model = "https://api.pranavakshit.in" + photo.imageUrl,
                                    contentDescription = "Photo",
                                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                                    contentScale = ContentScale.Crop
                                )
                                if (isSelected) {
                                    Box(modifier = Modifier.fillMaxSize().aspectRatio(1f).background(Color.Black.copy(alpha = 0.4f)))
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.align(Alignment.Center).size(36.dp)
                                    )
                                }
                                
                                if (photo.deletionStatus != "NONE") {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(4.dp)
                                            .background(Color.Red, shape = MaterialTheme.shapes.small)
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("!", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                
                                Row(modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)) {
                                    if (isAdmin) {
                                        if (photo.deletionStatus == "USER_REQUESTED") {
                                            IconButton(onClick = {
                                                coroutineScope.launch {
                                                    try {
                                                        val api = com.pranavakshit.gpscamportal.data.remote.ApiService.create(context)
                                                        api.approveDeletePhoto(photo.id)
                                                        fetchPhotos()
                                                    } catch (e: Exception) { e.printStackTrace() }
                                                }
                                            }) { Icon(Icons.Default.Check, contentDescription = "Approve", tint = Color.Green) }
                                            IconButton(onClick = {
                                                coroutineScope.launch {
                                                    try {
                                                        val api = com.pranavakshit.gpscamportal.data.remote.ApiService.create(context)
                                                        api.rejectDeletePhoto(photo.id)
                                                        fetchPhotos()
                                                    } catch (e: Exception) { e.printStackTrace() }
                                                }
                                            }) { Icon(Icons.Default.Close, contentDescription = "Reject", tint = Color.Red) }
                                        } else if (photo.deletionStatus == "NONE") {
                                            IconButton(onClick = {
                                                photoToRequestDelete = photo.id
                                            }) { Icon(Icons.Default.Warning, contentDescription = "Request Delete", tint = Color.Yellow) }
                                        } else if (photo.deletionStatus == "ADMIN_REQUESTED") {
                                             IconButton(onClick = {}) { Icon(Icons.Default.Info, contentDescription = "Pending User Approval", tint = Color.Gray) }
                                        } else if (photo.deletionStatus == "ADMIN_APPROVED") {
                                             IconButton(onClick = {}) { Icon(Icons.Default.Info, contentDescription = "Waiting for User Completion", tint = Color.Gray) }
                                        }
                                    } else {
                                        if (photo.deletionStatus == "ADMIN_REQUESTED") {
                                            IconButton(onClick = {
                                                coroutineScope.launch {
                                                    try {
                                                        val api = com.pranavakshit.gpscamportal.data.remote.ApiService.create(context)
                                                        api.approveDeletePhoto(photo.id) // User accepts admin request -> goes to soft delete
                                                        fetchPhotos()
                                                    } catch (e: Exception) { e.printStackTrace() }
                                                }
                                            }) { Icon(Icons.Default.Check, contentDescription = "Accept Request", tint = Color.Green) }
                                            IconButton(onClick = {
                                                coroutineScope.launch {
                                                    try {
                                                        val api = com.pranavakshit.gpscamportal.data.remote.ApiService.create(context)
                                                        api.rejectDeletePhoto(photo.id)
                                                        fetchPhotos()
                                                    } catch (e: Exception) { e.printStackTrace() }
                                                }
                                            }) { Icon(Icons.Default.Close, contentDescription = "Reject", tint = Color.Red) }
                                        } else if (photo.deletionStatus == "ADMIN_APPROVED") {
                                            IconButton(onClick = {
                                                photoToConfirmAction = Pair(photo.id, "complete")
                                            }) { Icon(Icons.Default.Delete, contentDescription = "Complete Delete", tint = Color.Red) }
                                            IconButton(onClick = {
                                                coroutineScope.launch {
                                                    try {
                                                        val api = com.pranavakshit.gpscamportal.data.remote.ApiService.create(context)
                                                        api.abortDeletePhoto(photo.id)
                                                        fetchPhotos()
                                                    } catch (e: Exception) { e.printStackTrace() }
                                                }
                                            }) { Icon(Icons.Default.Close, contentDescription = "Abort", tint = Color.LightGray) }
                                        } else if (photo.deletionStatus == "NONE") {
                                            IconButton(onClick = {
                                                photoToRequestDelete = photo.id
                                            }) { Icon(Icons.Default.Warning, contentDescription = "Request Delete", tint = Color.Yellow) }
                                        } else if (photo.deletionStatus == "USER_REQUESTED") {
                                            IconButton(onClick = {}) { Icon(Icons.Default.Info, contentDescription = "Pending Admin", tint = Color.Gray) }
                                        }
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

    // Full Screen Viewer
    if (viewingPhoto != null) {
        val photoData = viewingPhoto!!
        Dialog(
            onDismissRequest = { viewingPhoto = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                coil.compose.AsyncImage(
                    model = "https://api.pranavakshit.in" + photoData.imageUrl,
                    contentDescription = "Full Screen Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { viewingPhoto = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = Color.White)
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showDetailsDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Details", tint = Color.White)
                        Text("Details", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isAdmin) {
                            if (photoData.deletionStatus == "USER_REQUESTED") {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                                    coroutineScope.launch {
                                        val api = com.pranavakshit.gpscamportal.data.remote.ApiService.create(context)
                                        api.approveDeletePhoto(photoData.id)
                                        fetchPhotos()
                                        viewingPhoto = null
                                    }
                                }.padding(horizontal = 8.dp)) {
                                    Icon(Icons.Default.Check, contentDescription = "Approve", tint = Color.Green)
                                    Text("Approve", color = Color.Green, style = MaterialTheme.typography.labelSmall)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                                    coroutineScope.launch {
                                        val api = com.pranavakshit.gpscamportal.data.remote.ApiService.create(context)
                                        api.rejectDeletePhoto(photoData.id)
                                        fetchPhotos()
                                        viewingPhoto = null
                                    }
                                }.padding(horizontal = 8.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Reject", tint = Color.Red)
                                    Text("Reject", color = Color.Red, style = MaterialTheme.typography.labelSmall)
                                }
                            } else if (photoData.deletionStatus == "NONE") {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                                    photoToRequestDelete = photoData.id
                                }.padding(horizontal = 8.dp)) {
                                    Icon(Icons.Default.Warning, contentDescription = "Request", tint = Color.Yellow)
                                    Text("Req. Delete", color = Color.Yellow, style = MaterialTheme.typography.labelSmall)
                                }
                            } else if (photoData.deletionStatus == "ADMIN_REQUESTED") {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
                                    Icon(Icons.Default.Info, contentDescription = "Pending", tint = Color.Gray)
                                    Text("Pending User", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                }
                            } else if (photoData.deletionStatus == "ADMIN_APPROVED") {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
                                    Icon(Icons.Default.Info, contentDescription = "Pending", tint = Color.Gray)
                                    Text("Waiting Completion", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        } else {
                            if (photoData.deletionStatus == "ADMIN_REQUESTED") {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                                    coroutineScope.launch {
                                        val api = com.pranavakshit.gpscamportal.data.remote.ApiService.create(context)
                                        api.approveDeletePhoto(photoData.id)
                                        fetchPhotos()
                                        viewingPhoto = null
                                    }
                                }.padding(horizontal = 8.dp)) {
                                    Icon(Icons.Default.Check, contentDescription = "Accept", tint = Color.Green)
                                    Text("Accept Request", color = Color.Green, style = MaterialTheme.typography.labelSmall)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                                    coroutineScope.launch {
                                        val api = com.pranavakshit.gpscamportal.data.remote.ApiService.create(context)
                                        api.rejectDeletePhoto(photoData.id)
                                        fetchPhotos()
                                        viewingPhoto = null
                                    }
                                }.padding(horizontal = 8.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Reject", tint = Color.Red)
                                    Text("Reject", color = Color.Red, style = MaterialTheme.typography.labelSmall)
                                }
                            } else if (photoData.deletionStatus == "ADMIN_APPROVED") {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                                    photoToConfirmAction = Pair(photoData.id, "complete")
                                }.padding(horizontal = 8.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Complete", tint = Color.Red)
                                    Text("Complete", color = Color.Red, style = MaterialTheme.typography.labelSmall)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                                    coroutineScope.launch {
                                        val api = com.pranavakshit.gpscamportal.data.remote.ApiService.create(context)
                                        api.abortDeletePhoto(photoData.id)
                                        fetchPhotos()
                                        viewingPhoto = null
                                    }
                                }.padding(horizontal = 8.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Abort", tint = Color.LightGray)
                                    Text("Abort", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                                }
                            } else if (photoData.deletionStatus == "NONE") {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                                    photoToRequestDelete = photoData.id
                                }.padding(horizontal = 8.dp)) {
                                    Icon(Icons.Default.Warning, contentDescription = "Request", tint = Color.Yellow)
                                    Text("Req. Delete", color = Color.Yellow, style = MaterialTheme.typography.labelSmall)
                                }
                            } else if (photoData.deletionStatus == "USER_REQUESTED") {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
                                    Icon(Icons.Default.Info, contentDescription = "Pending", tint = Color.Gray)
                                    Text("Pending Admin", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (showDetailsDialog) {
            AlertDialog(
                onDismissRequest = { showDetailsDialog = false },
                title = { Text("Photo Details") },
                text = {
                    Column {
                        Text("Location: ${photoData.locationName}")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("GPS: ${photoData.latitude}, ${photoData.longitude}")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Time: ${photoData.timestamp}")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Uploader: ${photoData.uploader}")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Status: ${photoData.deletionStatus}")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDetailsDialog = false }) { Text("Close") }
                }
            )
        }
    }

    if (photoToRequestDelete != null) {
        AlertDialog(
            onDismissRequest = { photoToRequestDelete = null },
            title = { Text("Request Deletion") },
            text = {
                OutlinedTextField(
                    value = deleteReason,
                    onValueChange = { deleteReason = it },
                    label = { Text("Reason for deleting this photo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        try {
                            val api = com.pranavakshit.gpscamportal.data.remote.ApiService.create(context)
                            api.requestDeletePhoto(photoToRequestDelete!!, com.pranavakshit.gpscamportal.data.remote.DeletionRequest(reason = deleteReason))
                            fetchPhotos()
                            viewingPhoto = null
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                    photoToRequestDelete = null
                    deleteReason = ""
                }) { Text("Submit") }
            },
            dismissButton = {
                TextButton(onClick = { photoToRequestDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (photoToConfirmAction != null) {
        AlertDialog(
            onDismissRequest = { photoToConfirmAction = null },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to finalize deletion? This will move the photo to the Recycle Bin.") },
            confirmButton = {
                TextButton(onClick = {
                    val id = photoToConfirmAction!!.first
                    coroutineScope.launch {
                        try {
                            val api = com.pranavakshit.gpscamportal.data.remote.ApiService.create(context)
                            if (photoToConfirmAction!!.second == "complete") {
                                api.completeDeletePhoto(id)
                            } else {
                                api.deletePhoto(id)
                            }
                            fetchPhotos()
                            viewingPhoto = null
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                    photoToConfirmAction = null
                }) { Text("Confirm", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { photoToConfirmAction = null }) { Text("Cancel") }
            }
        )
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


