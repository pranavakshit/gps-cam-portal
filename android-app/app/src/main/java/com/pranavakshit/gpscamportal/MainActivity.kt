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
    LOGIN, DASHBOARD, LOCATION, CAMERA, OFFLINE_GALLERY
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
                    currentScreen = Screen.DASHBOARD
                }
            )
        }
        Screen.DASHBOARD -> {
            com.pranavakshit.gpscamportal.ui.screens.DashboardScreen(
                onNavigateToCamera = {
                    currentScreen = Screen.LOCATION
                },
                onNavigateToOfflineGallery = {
                    currentScreen = Screen.OFFLINE_GALLERY
                },
                onLogout = {
                    currentScreen = Screen.LOGIN
                }
            )
        }
        Screen.OFFLINE_GALLERY -> {
            com.pranavakshit.gpscamportal.ui.screens.GalleryScreen(
                onNavigateBack = {
                    currentScreen = Screen.DASHBOARD 
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
                    currentScreen = Screen.OFFLINE_GALLERY
                },
                onNavigateToGallery = {
                    currentScreen = Screen.OFFLINE_GALLERY
                }
            )
        }
    }
}