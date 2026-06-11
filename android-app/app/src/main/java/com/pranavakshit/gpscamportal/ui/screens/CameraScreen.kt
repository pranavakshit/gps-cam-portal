package com.pranavakshit.gpscamportal.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.*
import com.pranavakshit.gpscamportal.data.local.AppDatabase
import com.pranavakshit.gpscamportal.data.local.PhotoEntity
import com.pranavakshit.gpscamportal.util.ImageUtils
import com.pranavakshit.gpscamportal.util.LocationHelper
import com.pranavakshit.gpscamportal.util.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    state: String,
    district: String,
    area: String,
    onPhotoSaved: () -> Unit,
    onNavigateToGallery: () -> Unit
) {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    if (permissionsState.allPermissionsGranted) {
        CameraPreviewContent(state, district, area, onPhotoSaved, onNavigateToGallery)
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Camera and Location permissions are required to capture GPS stamped photos.")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                Text("Grant Permissions")
            }
        }
    }
}

@Composable
private fun CameraPreviewContent(
    state: String,
    district: String,
    area: String,
    onPhotoSaved: () -> Unit,
    onNavigateToGallery: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    
    var isCapturing by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = ContextCompat.getMainExecutor(ctx)
                
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    imageCapture = capture

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            capture
                        )
                    } catch (e: Exception) {
                        Log.e("CameraScreen", "Use case binding failed", e)
                    }
                }, executor)
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onNavigateToGallery) {
                Text("Gallery")
            }
            
            Button(
                onClick = {
                    if (isCapturing) return@Button
                    isCapturing = true
                    
                    val file = File(context.filesDir, "IMG_${System.currentTimeMillis()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

                    imageCapture?.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                coroutineScope.launch(Dispatchers.IO) {
                                    val locationHelper = LocationHelper(context)
                                    val loc = locationHelper.getCurrentLocation()
                                    val lat = loc?.latitude ?: 0.0
                                    val lng = loc?.longitude ?: 0.0
                                    val timestamp = System.currentTimeMillis()
                                    
                                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                                    
                                    val success = ImageUtils.addWatermarkAndSave(
                                        originalBitmap = bitmap,
                                        outputFile = file,
                                        area = area,
                                        district = district,
                                        state = state,
                                        latitude = lat,
                                        longitude = lng,
                                        timestamp = timestamp
                                    )
                                    
                                    if (success) {
                                        val uploader = UserPreferences(context).getUploaderName() ?: "Unknown"
                                        val dao = AppDatabase.getDatabase(context).photoDao()
                                        
                                        dao.insertPhoto(PhotoEntity(
                                            locationName = area,
                                            latitude = lat,
                                            longitude = lng,
                                            timestamp = timestamp,
                                            imageUri = file.absolutePath,
                                            uploader = uploader
                                        ))
                                        
                                        launch(Dispatchers.Main) {
                                            isCapturing = false
                                            onPhotoSaved()
                                        }
                                    } else {
                                        launch(Dispatchers.Main) {
                                            isCapturing = false
                                        }
                                    }
                                }
                            }

                            override fun onError(exc: ImageCaptureException) {
                                Log.e("CameraScreen", "Photo capture failed: ${exc.message}", exc)
                                isCapturing = false
                            }
                        }
                    )
                },
                enabled = !isCapturing
            ) {
                Text(if (isCapturing) "Saving..." else "Capture")
            }
        }
    }
}
