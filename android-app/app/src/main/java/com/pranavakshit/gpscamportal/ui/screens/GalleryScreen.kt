package com.pranavakshit.gpscamportal.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pranavakshit.gpscamportal.data.local.AppDatabase
import com.pranavakshit.gpscamportal.data.remote.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dao = remember { AppDatabase.getDatabase(context).photoDao() }
    
    val pendingPhotos by dao.getPendingUploads().collectAsState(initial = emptyList())
    var isUploading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline Gallery") },
                navigationIcon = {
                    Button(onClick = onNavigateBack, modifier = Modifier.padding(start = 8.dp)) {
                        Text("Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            if (pendingPhotos.isNotEmpty()) {
                BottomAppBar {
                    Button(
                        onClick = {
                            isUploading = true
                            coroutineScope.launch(Dispatchers.IO) {
                                val api = ApiService.create()
                                var uploadedCount = 0
                                
                                for (photo in pendingPhotos) {
                                    val file = File(photo.imageUri)
                                    if (!file.exists()) continue
                                    
                                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                                    val body = MultipartBody.Part.createFormData("photo", file.name, requestFile)

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
                                            dao.markAsUploaded(listOf(photo.id))
                                            uploadedCount++
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                
                                launch(Dispatchers.Main) {
                                    isUploading = false
                                    Toast.makeText(context, "Uploaded $uploadedCount photos", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        enabled = !isUploading
                    ) {
                        Text(if (isUploading) "Uploading..." else "Sync to Server (${pendingPhotos.size})")
                    }
                }
            }
        }
    ) { paddingValues ->
        if (pendingPhotos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("All photos are synced!")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pendingPhotos) { photo ->
                    Card {
                        Column {
                            AsyncImage(
                                model = photo.imageUri,
                                contentDescription = "Captured photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                text = photo.locationName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
