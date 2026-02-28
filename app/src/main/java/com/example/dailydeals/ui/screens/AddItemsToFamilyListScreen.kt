package com.example.dailydeals.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailydeals.R
import com.example.dailydeals.data.RetrofitClient
import com.example.dailydeals.data.SessionManager
import com.example.dailydeals.model.DetectedItem
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class
)
@Composable
fun AddItemsToFamilyListScreen(
    familyId: String,
    listId: Int,
    sessionManager: SessionManager,
    onNavigateBack: () -> Unit,
    onNavigateToBrandSelection: () -> Unit
) {
    var items by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var currentInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val apiService = remember { RetrofitClient.apiService }
    
    fun addItem() {
        val trimmed = currentInput.trim()
        if (trimmed.isNotEmpty() && !items.contains(trimmed)) {
            items = items + trimmed
            currentInput = ""
        }
    }
    
    fun processAndNavigate() {
        if (items.isEmpty()) return
        scope.launch {
            isLoading = true
            try {
                // Generate a temporary scanId
                val scanId = UUID.randomUUID().toString()
                
                // Convert string list to DetectedItem list
                val detectedItems = items.map { 
                    DetectedItem(
                        id = UUID.randomUUID().toString(),
                        name = it,
                        confidence = 1.0,
                        dealAvailable = true
                    )
                }
                
                // Confirm scan with backend to populate brands for this scanId
                apiService.confirmScan(scanId, detectedItems)
                
                // Save scanId so BrandSelectionScreen can pick it up
                sessionManager.saveLastScanId(scanId)
                
                // Navigate
                onNavigateToBrandSelection()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scan_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF007AFF))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF2F2F7),
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = Color(0xFFF2F2F7)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Input Region
            Surface(
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.scan_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = currentInput,
                            onValueChange = { currentInput = it },
                            placeholder = { Text(stringResource(R.string.scan_search_hint)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    addItem()
                                    keyboardController?.hide()
                                }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF007AFF),
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Button(
                            onClick = { addItem() },
                            modifier = Modifier.height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                        ) {
                            Text(stringResource(R.string.add_button))
                        }
                    }
                }
            }
            
            // List Region
            if (items.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.your_list_is_empty),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.start_typing_to_add_items),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    item,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                IconButton(
                                    onClick = { items = items.filter { it != item } },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Bottom Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { processAndNavigate() },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                stringResource(R.string.find_best_deals),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
