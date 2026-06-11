package com.pranavakshit.gpscamportal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.pranavakshit.gpscamportal.ui.screens.LocationScreen
import com.pranavakshit.gpscamportal.ui.screens.LoginScreen
import com.pranavakshit.gpscamportal.ui.screens.CameraScreen
import com.pranavakshit.gpscamportal.ui.theme.GPSCamPortalTheme
import com.pranavakshit.gpscamportal.util.UserPreferences

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GPSCamPortalTheme {
                GPSCamPortalApp()
            }
        }
    }
}

enum class Screen {
    LOGIN, LOCATION, CAMERA, GALLERY
}

@Composable
fun GPSCamPortalApp() {
    var currentScreen by remember { mutableStateOf(Screen.LOGIN) }
    
    // Store selected location temporarily before passing to Camera
    var selectedState by remember { mutableStateOf("") }
    var selectedDistrict by remember { mutableStateOf("") }
    var selectedArea by remember { mutableStateOf("") }

    when (currentScreen) {
        Screen.LOGIN -> {
            LoginScreen(
                onLoginSuccess = {
                    currentScreen = Screen.LOCATION
                }
            )
        }
        Screen.LOCATION -> {
            LocationScreen(
                onLocationSelected = { state, district, area ->
                    selectedState = state
                    selectedDistrict = district
                    selectedArea = area
                    currentScreen = Screen.CAMERA
                }
            )
        }
        Screen.CAMERA -> {
            CameraScreen(
                state = selectedState,
                district = selectedDistrict,
                area = selectedArea,
                onPhotoSaved = {
                    // Stay on Camera Screen to take more photos, or notify
                },
                onNavigateToGallery = {
                    currentScreen = Screen.GALLERY
                }
            )
        }
        Screen.GALLERY -> {
            com.pranavakshit.gpscamportal.ui.screens.GalleryScreen(
                onNavigateBack = {
                    currentScreen = Screen.CAMERA
                }
            )
        }
    }
}