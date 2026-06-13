package com.pranavakshit.gpscamportal.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pranavakshit.gpscamportal.data.remote.*
import com.pranavakshit.gpscamportal.util.UserPreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    onLocationSelected: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val scope = rememberCoroutineScope()
    
    var isLoading by remember { mutableStateOf(false) }
    var states by remember { mutableStateOf<List<StateDto>>(emptyList()) }
    
    var selectedState by remember { mutableStateOf<StateDto?>(null) }
    var selectedDistrict by remember { mutableStateOf<DistrictDto?>(null) }
    var selectedSubDistrict by remember { mutableStateOf<SubDistrictDto?>(null) }
    var selectedVillage by remember { mutableStateOf<VillageDto?>(null) }
    
    var stateExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var subDistrictExpanded by remember { mutableStateOf(false) }
    var villageExpanded by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchResultDto>>(emptyList()) }
    
    // Offline Bundle
    var currentBundle by remember { mutableStateOf<OfflineBundleDto?>(null) }

    // Initial Load: Fetch states, load cached bundle
    LaunchedEffect(Unit) {
        isLoading = true
        currentBundle = userPreferences.getOfflineBundle()
        if (currentBundle != null) {
            selectedState = currentBundle?.state
            states = listOf(currentBundle!!.state)
        }
        
        try {
            val apiService = ApiService.create(context)
            val response = apiService.getStates()
            if (response.isSuccessful && response.body() != null) {
                states = response.body()!!
            }
        } catch (e: Exception) {
            // Error loading states, fallback to cached state if exists
        } finally {
            isLoading = false
        }
    }

    // Effect for Search
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            if (selectedState == null) return@LaunchedEffect
            try {
                val apiService = ApiService.create(context)
                val response = apiService.searchLocations(searchQuery, selectedState!!.lgdCode)
                if (response.isSuccessful) {
                    searchResults = response.body() ?: emptyList()
                }
            } catch (e: Exception) {}
        } else {
            searchResults = emptyList()
        }
    }

    val fetchOfflineBundle = { state: StateDto ->
        scope.launch {
            isLoading = true
            try {
                val apiService = ApiService.create(context)
                val response = apiService.getOfflineBundle(state.lgdCode)
                if (response.isSuccessful && response.body() != null) {
                    val bundle = response.body()!!
                    userPreferences.saveOfflineBundle(bundle)
                    currentBundle = bundle
                    
                    // Reset lower selections
                    selectedDistrict = null
                    selectedSubDistrict = null
                    selectedVillage = null
                    searchQuery = ""
                    Toast.makeText(context, "State data downloaded successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to download state data", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Network error downloading state data", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Location") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // STATE
                ExposedDropdownMenuBox(
                    expanded = stateExpanded,
                    onExpandedChange = { stateExpanded = !stateExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedState?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("State") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = stateExpanded,
                        onDismissRequest = { stateExpanded = false }
                    ) {
                        states.forEach { state ->
                            DropdownMenuItem(
                                text = { Text(state.name) },
                                onClick = {
                                    if (selectedState?.lgdCode != state.lgdCode) {
                                        selectedState = state
                                        fetchOfflineBundle(state)
                                    }
                                    stateExpanded = false
                                }
                            )
                        }
                    }
                }

                // SEARCH
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        if (selectedState == null) {
                            Toast.makeText(context, "Please select a State first", Toast.LENGTH_SHORT).show()
                        } else {
                            searchQuery = it
                        }
                    },
                    label = { Text("Search Area (Auto-fills Dropdowns)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (searchResults.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
                    ) {
                        LazyColumn {
                            items(searchResults) { result ->
                                ListItem(
                                    headlineContent = { Text(result.name) },
                                    supportingContent = { Text(result.path) },
                                    modifier = Modifier.clickable {
                                        // Auto-fill logic
                                        if (result.districtId != null) {
                                            selectedDistrict = currentBundle?.districts?.find { it.id == result.districtId }
                                        }
                                        if (result.subDistrictId != null) {
                                            selectedSubDistrict = currentBundle?.subDistricts?.find { it.id == result.subDistrictId }
                                        }
                                        if (result.type == "Village") {
                                            selectedVillage = currentBundle?.villages?.find { it.id == result.id }
                                        } else if (result.type == "Ward") {
                                            // Handle Ward/ULB if needed, fallback to name
                                        }
                                        searchQuery = ""
                                        searchResults = emptyList()
                                    }
                                )
                            }
                        }
                    }
                }

                // DISTRICT
                ExposedDropdownMenuBox(
                    expanded = districtExpanded,
                    onExpandedChange = { if (selectedState != null) districtExpanded = !districtExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedDistrict?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("District") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        enabled = selectedState != null
                    )
                    ExposedDropdownMenu(
                        expanded = districtExpanded,
                        onDismissRequest = { districtExpanded = false }
                    ) {
                        currentBundle?.districts?.forEach { district ->
                            DropdownMenuItem(
                                text = { Text(district.name) },
                                onClick = {
                                    selectedDistrict = district
                                    selectedSubDistrict = null
                                    selectedVillage = null
                                    districtExpanded = false
                                }
                            )
                        }
                    }
                }

                // SUBDISTRICT
                ExposedDropdownMenuBox(
                    expanded = subDistrictExpanded,
                    onExpandedChange = { if (selectedDistrict != null) subDistrictExpanded = !subDistrictExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedSubDistrict?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("SubDistrict / ULB") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subDistrictExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        enabled = selectedDistrict != null
                    )
                    ExposedDropdownMenu(
                        expanded = subDistrictExpanded,
                        onDismissRequest = { subDistrictExpanded = false }
                    ) {
                        currentBundle?.subDistricts?.filter { it.districtCode == selectedDistrict?.lgdCode }?.forEach { subDistrict ->
                            DropdownMenuItem(
                                text = { Text(subDistrict.name) },
                                onClick = {
                                    selectedSubDistrict = subDistrict
                                    selectedVillage = null
                                    subDistrictExpanded = false
                                }
                            )
                        }
                    }
                }

                // VILLAGE
                ExposedDropdownMenuBox(
                    expanded = villageExpanded,
                    onExpandedChange = { if (selectedSubDistrict != null) villageExpanded = !villageExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedVillage?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Village / Ward") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = villageExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        enabled = selectedSubDistrict != null
                    )
                    ExposedDropdownMenu(
                        expanded = villageExpanded,
                        onDismissRequest = { villageExpanded = false }
                    ) {
                        currentBundle?.villages?.filter { it.subDistrictCode == selectedSubDistrict?.lgdCode }?.forEach { village ->
                            DropdownMenuItem(
                                text = { Text(village.name) },
                                onClick = {
                                    selectedVillage = village
                                    villageExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        if (selectedState != null && selectedDistrict != null && selectedVillage != null) {
                            onLocationSelected(selectedState!!.name, selectedDistrict!!.name, selectedVillage!!.name)
                        } else {
                            Toast.makeText(context, "Please complete all selections", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Start Camera")
                }
            }
        }
    }
}
