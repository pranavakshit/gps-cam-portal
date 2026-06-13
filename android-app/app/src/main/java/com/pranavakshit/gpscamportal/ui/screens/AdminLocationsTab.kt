package com.pranavakshit.gpscamportal.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pranavakshit.gpscamportal.data.remote.ApiService
import com.pranavakshit.gpscamportal.data.remote.StateDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

@Composable
fun AdminLocationsTab() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }
    
    var states by remember { mutableStateOf<List<StateDto>>(emptyList()) }
    var isLoadingStates by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val api = ApiService.create(context)
            val response = api.getStates()
            if (response.isSuccessful) {
                states = response.body() ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoadingStates = false
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        
        isUploading = true
        coroutineScope.launch {
            try {
                // Copy URI to a temp file
                val tempFile = withContext(Dispatchers.IO) {
                    val file = File(context.cacheDir, "lgd_upload_${System.currentTimeMillis()}.zip")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                    file
                }

                val requestFile = tempFile.asRequestBody("application/zip".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("zipfile", tempFile.name, requestFile)

                val api = ApiService.create(context)
                val response = api.importLocations(body)
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "LGD Data imported successfully", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Import failed: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }
                
                withContext(Dispatchers.IO) {
                    tempFile.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error uploading file", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isUploading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = { launcher.launch("application/zip") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            enabled = !isUploading
        ) {
            Icon(Icons.Default.Add, contentDescription = "Upload ZIP")
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isUploading) "Uploading..." else "Import LGD ZIP")
        }

        Text("Location Hierarchy", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        
        if (isLoadingStates) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (states.isEmpty()) {
            Text("No locations found.")
        } else {
            LazyColumn {
                items(states) { state ->
                    StateNode(state)
                }
            }
        }
    }
}

@Composable
fun StateNode(state: StateDto) {
    var expanded by remember { mutableStateOf(false) }
    var districts by remember { mutableStateOf<List<com.pranavakshit.gpscamportal.data.remote.DistrictDto>?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable {
                expanded = !expanded
                if (expanded && districts == null) {
                    coroutineScope.launch {
                        try {
                            val api = ApiService.create(context)
                            val res = api.getDistricts(state.lgdCode)
                            if (res.isSuccessful) districts = res.body()
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
            }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.List, contentDescription = "Folder")
            Spacer(modifier = Modifier.width(8.dp))
            Text(state.name, style = MaterialTheme.typography.bodyLarge)
        }
        if (expanded) {
            Box(modifier = Modifier.padding(start = 16.dp)) {
                if (districts == null) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                else if (districts!!.isEmpty()) Text("No districts", style = MaterialTheme.typography.bodySmall)
                else Column { districts!!.forEach { DistrictNode(it) } }
            }
        }
    }
}

@Composable
fun DistrictNode(district: com.pranavakshit.gpscamportal.data.remote.DistrictDto) {
    var expanded by remember { mutableStateOf(false) }
    var subDistricts by remember { mutableStateOf<List<com.pranavakshit.gpscamportal.data.remote.SubDistrictDto>?>(null) }
    var ulbs by remember { mutableStateOf<List<com.pranavakshit.gpscamportal.data.remote.UlbDto>?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable {
                expanded = !expanded
                if (expanded && subDistricts == null) {
                    coroutineScope.launch {
                        try {
                            val api = ApiService.create(context)
                            val resSD = api.getSubDistricts(district.lgdCode)
                            if (resSD.isSuccessful) subDistricts = resSD.body()
                            val resUlb = api.getUlbs(district.lgdCode)
                            if (resUlb.isSuccessful) ulbs = resUlb.body()
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
            }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.List, contentDescription = "Folder")
            Spacer(modifier = Modifier.width(8.dp))
            Text(district.name, style = MaterialTheme.typography.bodyMedium)
        }
        if (expanded) {
            Box(modifier = Modifier.padding(start = 16.dp)) {
                if (subDistricts == null && ulbs == null) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                else {
                    Column {
                        if (subDistricts != null) subDistricts!!.forEach { SubDistrictNode(it) }
                        if (ulbs != null) ulbs!!.forEach { UlbNode(it) }
                    }
                }
            }
        }
    }
}

@Composable
fun SubDistrictNode(subDistrict: com.pranavakshit.gpscamportal.data.remote.SubDistrictDto) {
    var expanded by remember { mutableStateOf(false) }
    var villages by remember { mutableStateOf<List<com.pranavakshit.gpscamportal.data.remote.VillageDto>?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable {
                expanded = !expanded
                if (expanded && villages == null) {
                    coroutineScope.launch {
                        try {
                            val api = ApiService.create(context)
                            val res = api.getVillages(subDistrict.lgdCode)
                            if (res.isSuccessful) villages = res.body()
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
            }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.List, contentDescription = "Folder")
            Spacer(modifier = Modifier.width(8.dp))
            Text(subDistrict.name, style = MaterialTheme.typography.bodyMedium)
        }
        if (expanded) {
            Box(modifier = Modifier.padding(start = 16.dp)) {
                if (villages == null) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                else if (villages!!.isEmpty()) Text("No villages", style = MaterialTheme.typography.bodySmall)
                else Column { villages!!.forEach { VillageNode(it) } }
            }
        }
    }
}

@Composable
fun VillageNode(village: com.pranavakshit.gpscamportal.data.remote.VillageDto) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.width(24.dp))
        Text(village.name, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun UlbNode(ulb: com.pranavakshit.gpscamportal.data.remote.UlbDto) {
    var expanded by remember { mutableStateOf(false) }
    var wards by remember { mutableStateOf<List<com.pranavakshit.gpscamportal.data.remote.WardDto>?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable {
                expanded = !expanded
                if (expanded && wards == null) {
                    coroutineScope.launch {
                        try {
                            val api = ApiService.create(context)
                            val res = api.getWards(ulb.lgdCode)
                            if (res.isSuccessful) wards = res.body()
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
            }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.List, contentDescription = "Folder")
            Spacer(modifier = Modifier.width(8.dp))
            Text(ulb.name + " (ULB)", style = MaterialTheme.typography.bodyMedium)
        }
        if (expanded) {
            Box(modifier = Modifier.padding(start = 16.dp)) {
                if (wards == null) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                else if (wards!!.isEmpty()) Text("No wards", style = MaterialTheme.typography.bodySmall)
                else Column { wards!!.forEach { WardNode(it) } }
            }
        }
    }
}

@Composable
fun WardNode(ward: com.pranavakshit.gpscamportal.data.remote.WardDto) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.width(24.dp))
        Text(ward.name, style = MaterialTheme.typography.bodySmall)
    }
}
