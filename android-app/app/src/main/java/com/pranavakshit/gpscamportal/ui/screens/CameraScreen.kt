package com.pranavakshit.gpscamportal.ui.screens

import android.Manifest
import android.graphics.BitmapFactory
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    var camera by remember { mutableStateOf<Camera?>(null) }
    
    var isCapturing by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_AUTO) }
    
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoomRatio by remember { mutableFloatStateOf(1f) }
    var minZoomRatio by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(lensFacing) {
        val cameraProvider = cameraProviderFuture.get()
        cameraProvider.unbindAll()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                previewView
            },
            update = { previewView ->
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setFlashMode(flashMode)
                    .build()
                imageCapture = capture

                val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                
                try {
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        capture
                    )
                    camera?.cameraInfo?.zoomState?.observe(lifecycleOwner) { state ->
                        maxZoomRatio = state.maxZoomRatio
                        minZoomRatio = state.minZoomRatio
                    }
                } catch (e: Exception) {
                    Log.e("CameraScreen", "Use case binding failed", e)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Top Bar Controls
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                flashMode = when (flashMode) {
                    ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                    ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                    else -> ImageCapture.FLASH_MODE_AUTO
                }
                imageCapture?.flashMode = flashMode
            }) {
                val flashIcon = when (flashMode) {
                    ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                    ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                    else -> Icons.Default.FlashOff
                }
                Icon(flashIcon, contentDescription = "Flash", tint = Color.White)
            }
            
            IconButton(onClick = {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
            }) {
                Icon(Icons.Default.Cameraswitch, contentDescription = "Switch Camera", tint = Color.White)
            }
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Zoom Slider
            if (maxZoomRatio > minZoomRatio) {
                Slider(
                    value = zoomRatio,
                    onValueChange = { 
                        zoomRatio = it
                        camera?.cameraControl?.setZoomRatio(it)
                    },
                    valueRange = minZoomRatio..maxZoomRatio,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingActionButton(onClick = onNavigateToGallery) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                }
                
                FloatingActionButton(
                    onClick = {
                        if (isCapturing) return@FloatingActionButton
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
                                            val uploader = UserPreferences(context).getUsername() ?: "Unknown"
                                            val dao = AppDatabase.getDatabase(context).photoDao()
                                            
                                            val entity = PhotoEntity()
                                            entity.locationName = "$area, $district, $state"
                                            entity.latitude = lat
                                            entity.longitude = lng
                                            entity.timestamp = timestamp
                                            entity.imageUri = file.absolutePath
                                            entity.uploader = uploader
                                            dao.insertPhoto(entity)
                                            
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
                    modifier = Modifier.size(80.dp)
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimaryContainer)
                    } else {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Capture", modifier = Modifier.size(40.dp))
                    }
                }
            }
        }
    }
}
