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
    val backStack = remember { androidx.compose.runtime.mutableStateListOf(Screen.LOGIN) }
    val currentScreen = backStack.lastOrNull() ?: Screen.LOGIN
    
    // Store selected location temporarily before passing to Camera
    var selectedState by remember { mutableStateOf("") }
    var selectedDistrict by remember { mutableStateOf("") }
    var selectedArea by remember { mutableStateOf("") }

    val context = androidx.compose.ui.platform.LocalContext.current
    var backPressedTime by remember { mutableStateOf(0L) }

    fun navigateTo(screen: Screen) {
        if (backStack.lastOrNull() != screen) {
            backStack.add(screen)
        }
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeLast()
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - backPressedTime < 2000) {
                (context as? android.app.Activity)?.finish()
            } else {
                android.widget.Toast.makeText(context, "Press back again to exit", android.widget.Toast.LENGTH_SHORT).show()
                backPressedTime = currentTime
            }
        }
    }

    androidx.activity.compose.BackHandler {
        navigateBack()
    }

    when (currentScreen) {
        Screen.LOGIN -> {
            LoginScreen(
                onLoginSuccess = {
                    backStack.clear()
                    backStack.add(Screen.DASHBOARD)
                }
            )
        }
        Screen.DASHBOARD -> {
            com.pranavakshit.gpscamportal.ui.screens.DashboardScreen(
                onNavigateToCamera = {
                    navigateTo(Screen.LOCATION)
                },
                onNavigateToOfflineGallery = {
                    navigateTo(Screen.OFFLINE_GALLERY)
                },
                onLogout = {
                    backStack.clear()
                    backStack.add(Screen.LOGIN)
                }
            )
        }
        Screen.OFFLINE_GALLERY -> {
            com.pranavakshit.gpscamportal.ui.screens.GalleryScreen(
                onNavigateBack = {
                    navigateBack()
                }
            )
        }
        Screen.LOCATION -> {
            LocationScreen(
                onLocationSelected = { state, district, area ->
                    selectedState = state
                    selectedDistrict = district
                    selectedArea = area
                    navigateTo(Screen.CAMERA)
                }
            )
        }
        Screen.CAMERA -> {
            CameraScreen(
                state = selectedState,
                district = selectedDistrict,
                area = selectedArea,
                onPhotoSaved = {
                    if (backStack.size > 1) backStack.removeLast() // pop location
                    if (backStack.size > 1) backStack.removeLast() // pop camera
                    navigateTo(Screen.OFFLINE_GALLERY)
                },
                onNavigateToGallery = {
                    if (backStack.size > 1) backStack.removeLast() // pop location
                    if (backStack.size > 1) backStack.removeLast() // pop camera
                    navigateTo(Screen.OFFLINE_GALLERY)
                }
            )
        }
    }
}