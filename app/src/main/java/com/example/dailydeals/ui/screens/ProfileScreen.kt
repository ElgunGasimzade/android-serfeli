package com.example.dailydeals.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailydeals.data.AuthService
import com.example.dailydeals.ui.components.BottomNavBar
import com.example.dailydeals.ui.theme.BackgroundCanvas
import com.example.dailydeals.ui.theme.Primary
import com.example.dailydeals.ui.utils.LocalScrollHandler
import kotlinx.coroutines.launch

import com.example.dailydeals.ui.utils.LocaleHelper
import android.app.Activity

@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val authService = remember { AuthService(context) }
    val scope = rememberCoroutineScope()
    
    // State mirroring iOS
    var username by remember { mutableStateOf(authService.getUser()?.username ?: "Guest") } 
    var email by remember { mutableStateOf(authService.getUser()?.email ?: "") }
    var phone by remember { mutableStateOf(authService.getUser()?.phone ?: "") }
    
    // Edit Mode State
    var isEditing by remember { mutableStateOf(false) }
    var isEditingUsername by remember { mutableStateOf(false) }
    var editEmail by remember { mutableStateOf(email) }
    var editPhone by remember { mutableStateOf(phone) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Username Edit Alert
    var newUsername by remember { mutableStateOf(username) }

    // Settings State (Persisted)
    val sessionManager = remember { com.example.dailydeals.data.SessionManager(context) }
    var selectedLanguage by remember { mutableStateOf(sessionManager.getLanguage()) }
    var locationEnabled by remember { mutableStateOf(sessionManager.isLocationEnabled()) }
    var searchRange by remember { mutableStateOf(sessionManager.getSearchRange()) }

    // Save settings whenever they change
    LaunchedEffect(selectedLanguage, locationEnabled, searchRange) {
        sessionManager.saveSettings(selectedLanguage, locationEnabled, searchRange)
    }
    
    // Scroll Handler (Tab 4)
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scrollHandler = com.example.dailydeals.ui.utils.LocalScrollHandler.current
    LaunchedEffect(Unit) {
        scrollHandler.scrollToTop.collect { tabIndex ->
            if (tabIndex == 4) {
                listState.animateScrollToItem(0)
            }
        }
    }

    // Fetch latest user profile from API on mount
    var isLoadingProfile by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isLoadingProfile = true
        try {
            // fetchUserProfile now handles the "force network" logic and propagates cancellation
            val freshProfile = authService.fetchUserProfile()
            if (freshProfile != null) {
                 // Prevent overwriting valid user data with Guest data
                 if (username != "Guest" && freshProfile.username == "Guest") {
                     // Ignore update
                 } else {
                     username = freshProfile.username
                 email = freshProfile.email ?: ""
                 phone = freshProfile.phone ?: ""
                 // Update edit fields too if not editing
                 if (!isEditing) {
                      editEmail = email
                      editPhone = phone
                 }
                 }
                 }

        } catch (e: kotlinx.coroutines.CancellationException) {
             throw e
        } catch (e: Exception) {
             e.printStackTrace()
        } finally {
             isLoadingProfile = false
        }
    }
    
    // Resume/Composition Check for Permissions
    // If the user disabled permission in Settings, the switch should turn OFF.
    androidx.compose.runtime.DisposableEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                 val fineLocation = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                 val coarseLocation = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                 val hasPermission = fineLocation == android.content.pm.PackageManager.PERMISSION_GRANTED || 
                                     coarseLocation == android.content.pm.PackageManager.PERMISSION_GRANTED
                 
                 // If permission is denied/revoked, we MUST set locationEnabled = false
                 if (!hasPermission && locationEnabled) {
                     locationEnabled = false
                 }
                 // Logic to auto-enable if permission IS granted is optional but user asked for "if location not enabled then show disabled"
                 // which implies strictly checking the negative case.
            }
        }
        val lifecycle = (context as? androidx.activity.ComponentActivity)?.lifecycle
        lifecycle?.addObserver(observer)
        onDispose {
            lifecycle?.removeObserver(observer)
        }
    }

    // Regex from iOS
    val emailRegex = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}".toRegex()

    // Location Permission Launcher for "Enable Location" switch
    val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                      permissions.getOrDefault(android.Manifest.permission.ACCESS_COARSE_LOCATION, false)
        if (granted) {
            locationEnabled = true
        } else {
            // Permission denied, switch remains off or turns off
            locationEnabled = false
        }
    }

    fun isValidEmail(email: String): Boolean {
        if (email.isEmpty()) return true
        return emailRegex.matches(email)
    }

    Scaffold(
        topBar = {
             @OptIn(ExperimentalMaterial3Api::class)
             CenterAlignedTopAppBar(
                 title = { Text(stringResource(com.example.dailydeals.R.string.profile_title), fontWeight = FontWeight.Bold) },
                 colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BackgroundCanvas)
             )
        },
        containerColor = BackgroundCanvas
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Avatar & Info Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .background(Color.LightGray.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Person, 
                            null, 
                            modifier = Modifier.size(50.dp),
                            tint = Color.Gray
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (!isEditing) {
                        // Username
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Username Display
                            Text(
                                username, 
                                style = MaterialTheme.typography.titleLarge, 
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(onClick = { 
                                isEditingUsername = true
                                newUsername = username
                            }, modifier = Modifier.size(24.dp).padding(start = 6.dp)) {
                                Icon(Icons.Filled.Edit, null, tint = Primary, modifier = Modifier.size(16.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))

                        // Email
                        if (email.isNotEmpty()) {
                            Text(email, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        } else {
                            Text(stringResource(com.example.dailydeals.R.string.no_email_set), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }

                        // Phone
                        if (phone.isNotEmpty()) {
                            Text(phone, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        } else {
                             Text(
                                stringResource(com.example.dailydeals.R.string.add_phone), 
                                color = Primary, 
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clickable { 
                                        isEditing = true 
                                        editEmail = email
                                        editPhone = phone
                                    }
                            )
                        }
                    } else {
                        Text(stringResource(com.example.dailydeals.R.string.editing_profile), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Edit Profile Form Section
            item {
                Text(
                    stringResource(com.example.dailydeals.R.string.header_personal_info), 
                    style = MaterialTheme.typography.labelSmall, 
                    color = Color.Gray, 
                    modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                ) {
                    if (isEditing) {
                        // Email Input Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(com.example.dailydeals.R.string.label_email), style = MaterialTheme.typography.bodyLarge, color = Color.Black, modifier = Modifier.width(100.dp))
                            androidx.compose.foundation.text.BasicTextField(
                                value = editEmail,
                                onValueChange = { editEmail = it },
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = if (editEmail.isEmpty()) Color.Gray else Color.Black),
                                decorationBox = { innerTextField ->
                                    if (editEmail.isEmpty()) Text(stringResource(com.example.dailydeals.R.string.hint_required), color = Color.Gray.copy(alpha = 0.5f))
                                    innerTextField()
                                }
                            )
                        }
                        
                        Divider(modifier = Modifier.padding(start = 16.dp), color = Color.Gray.copy(alpha = 0.2f))
                        
                        // Phone Input Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(com.example.dailydeals.R.string.label_phone), style = MaterialTheme.typography.bodyLarge, color = Color.Black, modifier = Modifier.width(100.dp))
                            androidx.compose.foundation.text.BasicTextField(
                                value = editPhone,
                                onValueChange = { editPhone = it },
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = if (editPhone.isEmpty()) Color.Gray else Color.Black),
                                decorationBox = { innerTextField ->
                                    if (editPhone.isEmpty()) Text(stringResource(com.example.dailydeals.R.string.hint_optional), color = Color.Gray.copy(alpha = 0.5f))
                                    innerTextField()
                                }
                            )
                        }

                        if (errorMessage != null) {
                            Divider(modifier = Modifier.padding(start = 16.dp), color = Color.Gray.copy(alpha = 0.2f))
                            Text(
                                errorMessage!!, 
                                color = Color.Red, 
                                style = MaterialTheme.typography.bodySmall, 
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        
                        Divider(modifier = Modifier.padding(start = 16.dp), color = Color.Gray.copy(alpha = 0.2f))

                        // Actions Row (Save)
                        Button(
                            onClick = { 
                                if (!isValidEmail(editEmail)) {
                                    errorMessage = context.getString(com.example.dailydeals.R.string.invalid_email)
                                    return@Button
                                }
                                scope.launch {
                                    try {
                                        // Fix: Pass newUsername, not the old 'username' state
                                        val success = authService.updateProfile(newUsername, editEmail, editPhone)
                                        if (success) {
                                            username = newUsername // Update local state on success
                                            email = editEmail
                                            phone = editPhone
                                            isEditing = false
                                            errorMessage = null
                                            // Ideally show a snackbar here, but for now we rely on UI state change
                                        } else {
                                            errorMessage = context.getString(com.example.dailydeals.R.string.update_failed)
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Unable to connect. Please try again."
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = androidx.compose.ui.graphics.RectangleShape
                        ) {
                            Text(stringResource(com.example.dailydeals.R.string.save_changes), color = Primary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Divider(modifier = Modifier.padding(start = 16.dp), color = Color.Gray.copy(alpha = 0.2f))

                        // Actions Row (Cancel)
                        Button(
                            onClick = { isEditing = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = androidx.compose.ui.graphics.RectangleShape
                        ) {
                            Text(stringResource(com.example.dailydeals.R.string.cancel), color = Color.Red, fontSize = 17.sp)
                        }

                    } else {
                         // Read Only Row
                         Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    editEmail = email
                                    editPhone = phone
                                    isEditing = true
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(com.example.dailydeals.R.string.edit_contact_info), style = MaterialTheme.typography.bodyLarge, color = Primary)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Filled.Edit, null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            
            // Language Settings
            item {
                Text(
                    stringResource(com.example.dailydeals.R.string.header_language), 
                    style = MaterialTheme.typography.labelSmall, 
                    color = Color.Gray, 
                    modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                ) {
                    // Custom Segmented Control
                    Box(modifier = Modifier.padding(16.dp)) {
                         Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEEEEEF), RoundedCornerShape(8.dp))
                                .padding(2.dp),
                             horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // English
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .shadow(if (selectedLanguage == "en") 1.dp else 0.dp, RoundedCornerShape(6.dp))
                                    .background(if (selectedLanguage == "en") Color.White else Color.Transparent, RoundedCornerShape(6.dp))
                                    .clickable { 
                                        if (selectedLanguage != "en") {
                                            selectedLanguage = "en"
                                            LocaleHelper.setLocale(context, "en")
                                            (context as? Activity)?.recreate()
                                        }
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(stringResource(com.example.dailydeals.R.string.lang_english), fontWeight = if (selectedLanguage == "en") FontWeight.SemiBold else FontWeight.Normal, fontSize = 13.sp)
                            }
                            
                            // Azerbaijani
                             Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .shadow(if (selectedLanguage == "az") 1.dp else 0.dp, RoundedCornerShape(6.dp))
                                    .background(if (selectedLanguage == "az") Color.White else Color.Transparent, RoundedCornerShape(6.dp))
                                    .clickable { 
                                        if (selectedLanguage != "az") {
                                            selectedLanguage = "az"
                                            LocaleHelper.setLocale(context, "az")
                                            (context as? Activity)?.recreate()
                                        }
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(stringResource(com.example.dailydeals.R.string.lang_az), fontWeight = if (selectedLanguage == "az") FontWeight.SemiBold else FontWeight.Normal, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Location Settings
            item {
                Text(
                    stringResource(com.example.dailydeals.R.string.header_location_settings), 
                    style = MaterialTheme.typography.labelSmall, 
                    color = Color.Gray, 
                    modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                ) {
                    // Enable Location Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(com.example.dailydeals.R.string.enable_location), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Switch(
                            checked = locationEnabled, 
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    // Verify permission before enabling
                                    if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                                        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                        locationEnabled = true
                                    } else {
                                        // Ask for permission
                                        // We need a launcher. Since we are inside a callback, we use a SideEffect or a launcher trigger?
                                        // Creating launcher at top level and calling launch() here is correct.
                                        // BUT standard way is mutableState trigger or direct launch if launcher val is available.
                                        // We will define launcher above.
                                        // "locationPermissionLauncher" needs to be defined at top.
                                        // We'll update this block after defining launcher.
                                        // Since replace_file_content replaces a chunk, we must ensure launcher is defined.
                                        // We will add launcher definition at top of ProfileScreen and update this switch to use it.
                                        // ERROR: I cannot edit top of file and this block in one go unless I use MultiReplace.
                                        // I will use MultiReplace to add launcher AND update switch.
                                        locationPermissionLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
                                    }
                                } else {
                                    locationEnabled = false
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = com.example.dailydeals.ui.theme.SystemGreen)
                        )
                    }
                    
                    if (locationEnabled) {
                        Divider(modifier = Modifier.padding(start = 16.dp), color = Color.Gray.copy(alpha = 0.2f))
                        
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(), 
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(com.example.dailydeals.R.string.search_range), style = MaterialTheme.typography.bodyMedium)
                                Text("${searchRange.toInt()} km", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Slider(
                                value = searchRange,
                                onValueChange = { searchRange = it },
                                valueRange = 1f..20f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White, 
                                    activeTrackColor = Primary,
                                    inactiveTrackColor = Color(0xFFE5E5EA) // iOS Gray5
                                )
                            )
                        }
                    }
                }
            }
            
            // Spacer for scroll comfort
             item { Spacer(modifier = Modifier.height(40.dp)) }
        }
        
        if (isEditingUsername) {
            AlertDialog(
                onDismissRequest = { isEditingUsername = false },
                title = { Text(stringResource(com.example.dailydeals.R.string.edit_username_title)) },
                text = {
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text(stringResource(com.example.dailydeals.R.string.label_username)) },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                val success = authService.updateProfile(newUsername, email, phone)
                                if (success) {
                                    username = newUsername
                                    isEditingUsername = false
                                }
                            }
                        }
                    ) {
                        Text(stringResource(com.example.dailydeals.R.string.save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isEditingUsername = false }) {
                        Text(stringResource(com.example.dailydeals.R.string.cancel))
                    }
                }
            )
        }
    }
}



