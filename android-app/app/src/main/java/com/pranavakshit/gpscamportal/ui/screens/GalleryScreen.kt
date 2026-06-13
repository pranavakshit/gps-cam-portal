package com.pranavakshit.gpscamportal.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.pranavakshit.gpscamportal.data.local.AppDatabase
import com.pranavakshit.gpscamportal.data.local.PhotoEntity
import com.pranavakshit.gpscamportal.data.remote.ApiService
import com.pranavakshit.gpscamportal.data.remote.DeletionRequest
import com.pranavakshit.gpscamportal.data.remote.PhotoDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.pranavakshit.gpscamportal.util.UserPreferences

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dao = remember { AppDatabase.getDatabase(context).photoDao() }
    
    val pendingPhotos by dao.getPendingUploads().observeAsState(initial = emptyList())
    val deletedOfflinePhotos by dao.getDeletedPhotos().observeAsState(initial = emptyList())
    var cloudPhotos by remember { mutableStateOf<List<PhotoDto>>(emptyList()) }
    var recycleBinPhotos by remember { mutableStateOf<List<PhotoDto>>(emptyList()) }
    
    var isUploading by remember { mutableStateOf(false) }
    var isLoadingCloud by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Offline, 1 = Cloud, 2 = Recycle Bin

    val userPreferences = remember { UserPreferences(context) }
    val role = userPreferences.getRole() ?: "user"
    val isAdmin = role.equals("ADMIN", ignoreCase = true)

    // Multi-Select State
    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedOfflineIds = remember { mutableStateListOf<Int>() }
    val selectedCloudIds = remember { mutableStateListOf<Int>() }

    // Full Screen Viewer State
    var viewingOfflinePhoto by remember { mutableStateOf<PhotoEntity?>(null) }
    var viewingCloudPhoto by remember { mutableStateOf<PhotoDto?>(null) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    
    // Dialog states
    var showReasonDialog by remember { mutableStateOf(false) }
    var deleteReason by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // Pair(Action, isCloud) where Action = "delete" | "hard_delete"

    BackHandler(enabled = true) {
        if (isMultiSelectMode) {
            isMultiSelectMode = false
            selectedOfflineIds.clear()
            selectedCloudIds.clear()
        } else {
            onNavigateBack()
        }
    }

    val fetchCloudPhotos = {
        coroutineScope.launch {
            isLoadingCloud = true
            try {
                val api = ApiService.create(context)
                val response = api.getPhotos(recycleBin = false)
                if (response.isSuccessful) {
                    cloudPhotos = response.body() ?: emptyList()
                }
                val recycleResponse = api.getPhotos(recycleBin = true)
                if (recycleResponse.isSuccessful) {
                    recycleBinPhotos = recycleResponse.body() ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingCloud = false
            }
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab > 0 && cloudPhotos.isEmpty() && recycleBinPhotos.isEmpty()) {
            fetchCloudPhotos()
        }
    }

    fun toggleSelection(id: Int, isOffline: Boolean) {
        if (isOffline) {
            if (selectedOfflineIds.contains(id)) selectedOfflineIds.remove(id)
            else selectedOfflineIds.add(id)
            if (selectedOfflineIds.isEmpty() && selectedCloudIds.isEmpty()) isMultiSelectMode = false
        } else {
            if (selectedCloudIds.contains(id)) selectedCloudIds.remove(id)
            else selectedCloudIds.add(id)
            if (selectedOfflineIds.isEmpty() && selectedCloudIds.isEmpty()) isMultiSelectMode = false
        }
    }

    Scaffold(
        topBar = {
            if (isMultiSelectMode) {
                TopAppBar(
                    title = { Text("${selectedOfflineIds.size + selectedCloudIds.size} Selected") },
                    navigationIcon = {
                        IconButton(onClick = {
                            isMultiSelectMode = false
                            selectedOfflineIds.clear()
                            selectedCloudIds.clear()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            // Bulk Download logic here if needed
                            Toast.makeText(context, "Bulk download not fully implemented", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Download, contentDescription = "Download")
                        }
                        IconButton(onClick = {
                            coroutineScope.launch {
                                // Soft Delete selected offline
                                selectedOfflineIds.forEach { id ->
                                    dao.softDeletePhoto(id)
                                }
                                selectedOfflineIds.clear()

                                // Request delete for selected cloud
                                val api = ApiService.create(context)
                                selectedCloudIds.forEach { id ->
                                    val photo = cloudPhotos.find { it.id == id } ?: recycleBinPhotos.find { it.id == id }
                                    if (photo != null) {
                                        if (isAdmin) {
                                            if (photo.deletionStatus == "NONE") {
                                                api.requestDeletePhoto(id, DeletionRequest(reason = "Bulk Deletion Request (Admin)"))
                                            } else if (photo.deletionStatus == "USER_REQUESTED") {
                                                api.approveDeletePhoto(id)
                                            }
                                        } else {
                                            if (photo.deletionStatus == "ADMIN_APPROVED") {
                                                api.completeDeletePhoto(id)
                                            } else if (photo.deletionStatus == "ADMIN_REQUESTED") {
                                                api.approveDeletePhoto(id)
                                            } else if (photo.deletionStatus == "NONE") {
                                                api.requestDeletePhoto(id, DeletionRequest(reason = "Bulk Deletion Request (User)"))
                                            }
                                        }
                                    }
                                }
                                selectedCloudIds.clear()
                                isMultiSelectMode = false
                                Toast.makeText(context, "Bulk deletion/request processed", Toast.LENGTH_SHORT).show()
                                fetchCloudPhotos()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            } else {
                Column {
                    TopAppBar(
                        title = { Text("Unified Gallery") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0; isMultiSelectMode = false },
                            text = { Text("Offline (${pendingPhotos.size})") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1; isMultiSelectMode = false },
                            text = { Text("Cloud") }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2; isMultiSelectMode = false },
                            text = { Text("Recycle Bin") }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isMultiSelectMode) {
                FloatingActionButton(onClick = onNavigateBack) {
                    Text("📷")
                }
            }
        },
        bottomBar = {
            if (selectedTab == 0 && pendingPhotos.isNotEmpty() && !isMultiSelectMode) {
                BottomAppBar {
                    Button(
                        onClick = {
                            isUploading = true
                            coroutineScope.launch(Dispatchers.IO) {
                                val api = ApiService.create(context)
                                var uploadedCount = 0
                                var failedCount = 0
                                var lastErrorMsg = ""
                                
                                for (photo in pendingPhotos) {
                                    val file = File(photo.imageUri)
                                    if (!file.exists()) continue
                                    
                                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                                    val body = MultipartBody.Part.createFormData("photos", file.name, requestFile)

                                    val uploaderBody = photo.uploader.toRequestBody("text/plain".toMediaTypeOrNull())
                                    val locNameBody = photo.locationName.toRequestBody("text/plain".toMediaTypeOrNull())
                                    val latBody = photo.latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                                    val lngBody = photo.longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                                    
                                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    val timeStr = dateFormat.format(Date(photo.timestamp))
                                    val timeBody = timeStr.toRequestBody("text/plain".toMediaTypeOrNull())

                                    try {
                                        val response = api.uploadPhoto(uploaderBody, locNameBody, latBody, lngBody, timeBody, body)
                                        if (response.isSuccessful) {
                                            val ids = listOf(photo.id)
                                            dao.markAsUploaded(ids)
                                            uploadedCount++
                                        } else {
                                            lastErrorMsg = "Server error ${response.code()}"
                                            failedCount++
                                        }
                                    } catch (e: Exception) {
                                        lastErrorMsg = e.localizedMessage ?: "Unknown error"
                                        failedCount++
                                    }
                                }
                                
                                launch(Dispatchers.Main) {
                                    isUploading = false
                                    if (failedCount > 0) {
                                        Toast.makeText(context, "Uploaded $uploadedCount. Failed: $failedCount. Error: $lastErrorMsg", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Uploaded $uploadedCount photos", Toast.LENGTH_SHORT).show()
                                    }
                                    fetchCloudPhotos()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        enabled = !isUploading
                    ) {
                        Text(if (isUploading) "Uploading..." else "Sync to Server (${pendingPhotos.size})")
                    }
                }
            }
        }
    ) { paddingValues ->
        val currentPhotos = when (selectedTab) {
            0 -> pendingPhotos
            1 -> cloudPhotos
            else -> recycleBinPhotos + deletedOfflinePhotos.map { 
                PhotoDto(
                    id = it.id, 
                    locationName = it.locationName, 
                    stateCode = null, districtCode = null, 
                    latitude = it.latitude, longitude = it.longitude, 
                    timestamp = it.timestamp.toString(), 
                    imageUrl = it.imageUri, // using local URI
                    uploader = it.uploader,
                    deletionStatus = "DELETED_SOFT",
                    deletionReason = "Deleted offline"
                ) 
            }
        }

        if (selectedTab > 0 && isLoadingCloud) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (currentPhotos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(if (selectedTab == 0) "All offline photos are synced!" else "No photos found.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (selectedTab == 0) {
                    items(pendingPhotos) { photo ->
                        val isSelected = selectedOfflineIds.contains(photo.id)
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .combinedClickable(
                                    onClick = {
                                        if (isMultiSelectMode) {
                                            toggleSelection(photo.id, true)
                                        } else {
                                            viewingOfflinePhoto = photo
                                        }
                                    },
                                    onLongClick = {
                                        isMultiSelectMode = true
                                        toggleSelection(photo.id, true)
                                    }
                                )
                        ) {
                            AsyncImage(
                                model = photo.imageUri,
                                contentDescription = "Offline Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            if (isSelected) {
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.Center).size(36.dp)
                                )
                            }
                        }
                    }
                } else {
                    val photosList = if (selectedTab == 1) cloudPhotos else recycleBinPhotos
                    items(photosList) { photo ->
                        val isSelected = selectedCloudIds.contains(photo.id)
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .combinedClickable(
                                    onClick = {
                                        if (isMultiSelectMode) {
                                            toggleSelection(photo.id, false)
                                        } else {
                                            viewingCloudPhoto = photo
                                        }
                                    },
                                    onLongClick = {
                                        isMultiSelectMode = true
                                        toggleSelection(photo.id, false)
                                    }
                                )
                        ) {
                            AsyncImage(
                                model = "https://api.pranavakshit.in" + photo.imageUrl,
                                contentDescription = "Cloud Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            if (isSelected) {
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.Center).size(36.dp)
                                )
                            }
                            
                            // Status indicator for pending deletion
                            if (photo.deletionStatus == "USER_REQUESTED" || photo.deletionStatus == "ADMIN_APPROVED" || photo.deletionStatus == "ADMIN_REQUESTED") {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .background(Color.Red, shape = MaterialTheme.shapes.small)
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("!", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Full Screen Viewer
    if (viewingOfflinePhoto != null || viewingCloudPhoto != null) {
        val isOffline = viewingOfflinePhoto != null
        val modelUrl = if (isOffline) viewingOfflinePhoto!!.imageUri else "https://api.pranavakshit.in" + viewingCloudPhoto!!.imageUrl
        val locationName = if (isOffline) viewingOfflinePhoto!!.locationName else viewingCloudPhoto!!.locationName
        val uploader = if (isOffline) viewingOfflinePhoto!!.uploader else viewingCloudPhoto!!.uploader
        val lat = if (isOffline) viewingOfflinePhoto!!.latitude else viewingCloudPhoto!!.latitude
        val lng = if (isOffline) viewingOfflinePhoto!!.longitude else viewingCloudPhoto!!.longitude
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val timeStr = if (isOffline) dateFormat.format(Date(viewingOfflinePhoto!!.timestamp)) else viewingCloudPhoto!!.timestamp

        Dialog(
            onDismissRequest = { 
                viewingOfflinePhoto = null
                viewingCloudPhoto = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                AsyncImage(
                    model = modelUrl,
                    contentDescription = "Full Screen Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { viewingOfflinePhoto = null; viewingCloudPhoto = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Bottom Actions
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
                    
                    if (selectedTab != 2) {
                        if (isOffline) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                                showConfirmDialog = Pair("delete", false)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                Text("Delete", color = Color.Red, style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            val cloudData = viewingCloudPhoto!!
                            if (isAdmin) {
                                if (cloudData.deletionStatus == "USER_REQUESTED") {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                                        coroutineScope.launch {
                                            val api = ApiService.create(context)
                                            api.approveDeletePhoto(cloudData.id)
                                            fetchCloudPhotos()
                                            viewingCloudPhoto = null
                                        }
                                    }.padding(horizontal = 8.dp)) {
                                        Icon(Icons.Default.Check, contentDescription = "Approve", tint = Color.Green)
                                        Text("Approve", color = Color.Green, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                                        coroutineScope.launch {
                                            val api = ApiService.create(context)
                                            api.rejectDeletePhoto(cloudData.id)
                                            fetchCloudPhotos()
                                            viewingCloudPhoto = null
                                        }
                                    }.padding(horizontal = 8.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Reject", tint = Color.Red)
                                        Text("Reject", color = Color.Red, style = MaterialTheme.typography.labelSmall)
                                    }
                                } else if (cloudData.deletionStatus == "NONE") {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                                        showReasonDialog = true
                                    }.padding(horizontal = 8.dp)) {
                                        Icon(Icons.Default.Warning, contentDescription = "Request", tint = Color.Yellow)
                                        Text("Req. Delete", color = Color.Yellow, style = MaterialTheme.typography.labelSmall)
                                    }
                                } else if (cloudData.deletionStatus == "ADMIN_REQUESTED") {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
                                        Icon(Icons.Default.Info, contentDescription = "Pending User", tint = Color.Gray)
                                        Text("Pending User", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                    }
                                } else if (cloudData.deletionStatus == "ADMIN_APPROVED") {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
                                        Icon(Icons.Default.Info, contentDescription = "Waiting Completion", tint = Color.Gray)
                                        Text("Waiting Completion", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            } else {
                                if (cloudData.deletionStatus == "ADMIN_REQUESTED") {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                                        coroutineScope.launch {
                                            val api = ApiService.create(context)
                                            api.approveDeletePhoto(cloudData.id)
                                            fetchCloudPhotos()
                                            viewingCloudPhoto = null
                                        }
                                    }.padding(horizontal = 8.dp)) {
                                        Icon(Icons.Default.Check, contentDescription = "Accept Request", tint = Color.Green)
                                        Text("Accept", color = Color.Green, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                                        coroutineScope.launch {
                                            val api = ApiService.create(context)
                                            api.rejectDeletePhoto(cloudData.id)
                                            fetchCloudPhotos()
                                            viewingCloudPhoto = null
                                        }
                                    }.padding(horizontal = 8.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Reject", tint = Color.Red)
                                        Text("Reject", color = Color.Red, style = MaterialTheme.typography.labelSmall)
                                    }
                                } else if (cloudData.deletionStatus == "ADMIN_APPROVED") {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                                        showConfirmDialog = Pair("complete", true)
                                    }.padding(horizontal = 8.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Complete Delete", tint = Color.Red)
                                        Text("Complete", color = Color.Red, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                                        coroutineScope.launch {
                                            val api = ApiService.create(context)
                                            api.abortDeletePhoto(cloudData.id)
                                            fetchCloudPhotos()
                                            viewingCloudPhoto = null
                                        }
                                    }.padding(horizontal = 8.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Abort", tint = Color.LightGray)
                                        Text("Abort", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                                    }
                                } else if (cloudData.deletionStatus == "NONE") {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                                        showReasonDialog = true
                                    }.padding(horizontal = 8.dp)) {
                                        Icon(Icons.Default.Warning, contentDescription = "Request Delete", tint = Color.Yellow)
                                        Text("Req. Delete", color = Color.Yellow, style = MaterialTheme.typography.labelSmall)
                                    }
                                } else if (cloudData.deletionStatus == "USER_REQUESTED") {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
                                        Icon(Icons.Default.Info, contentDescription = "Pending Admin", tint = Color.Gray)
                                        Text("Pending Admin", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                            if (isOffline) {
                                coroutineScope.launch {
                                    dao.restorePhoto(viewingOfflinePhoto!!.id)
                                    viewingOfflinePhoto = null
                                }
                            } else {
                                coroutineScope.launch {
                                    val api = ApiService.create(context)
                                    val res = api.restorePhoto(viewingCloudPhoto!!.id)
                                    if (res.isSuccessful) {
                                        Toast.makeText(context, "Photo restored", Toast.LENGTH_SHORT).show()
                                        fetchCloudPhotos()
                                        viewingCloudPhoto = null
                                    }
                                }
                            }
                        }.padding(horizontal = 8.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Restore", tint = Color.Green)
                            Text("Restore", color = Color.Green, style = MaterialTheme.typography.labelSmall)
                        }
                        if (isAdmin || isOffline) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                                showConfirmDialog = Pair("hard_delete", !isOffline)
                            }.padding(horizontal = 8.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Permanently Delete", tint = Color.Red)
                                Text("Perm. Delete", color = Color.Red, style = MaterialTheme.typography.labelSmall)
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
                        Text("Location: $locationName")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Device GPS: $lat, $lng")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Time: $timeStr")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Uploader: $uploader")
                        if (!isOffline) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Deletion Status: ${viewingCloudPhoto?.deletionStatus ?: "NONE"}")
                            if (!viewingCloudPhoto?.deletionReason.isNullOrEmpty()) {
                                Text("Reason: ${viewingCloudPhoto?.deletionReason}")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDetailsDialog = false }) { Text("Close") }
                }
            )
        }
        
        if (showReasonDialog) {
            AlertDialog(
                onDismissRequest = { showReasonDialog = false },
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
                                val api = ApiService.create(context)
                                api.requestDeletePhoto(viewingCloudPhoto!!.id, DeletionRequest(reason = deleteReason))
                                fetchCloudPhotos()
                                viewingCloudPhoto = null
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                        showReasonDialog = false
                        deleteReason = ""
                    }) { Text("Submit") }
                },
                dismissButton = {
                    TextButton(onClick = { showReasonDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showConfirmDialog != null) {
            val action = showConfirmDialog!!.first
            val isCloud = showConfirmDialog!!.second
            AlertDialog(
                onDismissRequest = { showConfirmDialog = null },
                title = { Text(if (action == "hard_delete") "Permanently Delete" else "Confirm Deletion") },
                text = { 
                    Text(if (action == "hard_delete") "Are you sure you want to PERMANENTLY delete this photo? This cannot be undone." else "Are you sure you want to finalize deletion? This will move the photo to the Recycle Bin.") 
                },
                confirmButton = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            try {
                                if (action == "hard_delete") {
                                    if (isCloud) {
                                        val api = ApiService.create(context)
                                        api.hardDeletePhoto(viewingCloudPhoto!!.id)
                                        fetchCloudPhotos()
                                        viewingCloudPhoto = null
                                    } else {
                                        val file = File(viewingOfflinePhoto!!.imageUri)
                                        if (file.exists()) file.delete()
                                        dao.deletePhoto(viewingOfflinePhoto!!.id)
                                        viewingOfflinePhoto = null
                                    }
                                } else {
                                    if (isCloud) {
                                        val api = ApiService.create(context)
                                        if (action == "complete") {
                                            api.completeDeletePhoto(viewingCloudPhoto!!.id)
                                        } else {
                                            api.deletePhoto(viewingCloudPhoto!!.id)
                                        }
                                        fetchCloudPhotos()
                                        viewingCloudPhoto = null
                                    } else {
                                        dao.softDeletePhoto(viewingOfflinePhoto!!.id)
                                        viewingOfflinePhoto = null
                                    }
                                }
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                        showConfirmDialog = null
                    }) { Text("Confirm", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = null }) { Text("Cancel") }
                }
            )
        }
    }
}
