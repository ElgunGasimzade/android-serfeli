package app.serfeli.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import app.serfeli.data.RetrofitClient
import app.serfeli.model.DetectedItem
import app.serfeli.ui.theme.Primary
import app.serfeli.ui.theme.TextMain
import kotlinx.coroutines.launch
import app.serfeli.ui.components.BottomNavBar
import java.util.UUID

@Composable
fun ScanCaptureScreen(
    onNavigateToDeals: () -> Unit,
    onNavigate: (String) -> Unit
) {
    // Parity with iOS: Logic is "Create Your List" / Manual Entry + Search
    // No Camera Preview.
    
    val apiService = remember { RetrofitClient.apiService }
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { app.serfeli.data.SessionManager(context) }
    val scope = rememberCoroutineScope()
        
    var searchText by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var shoppingListItems by remember { mutableStateOf<List<DetectedItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // Search Logic (Debounced)
    LaunchedEffect(Unit) {
        // Load items added from ProductDetails (or previous sessions)
        val savedStrings = sessionManager.getShoppingList()
        if (savedStrings.isNotEmpty()) {
             val convertedItems = savedStrings.map { name ->
                 DetectedItem(
                     id = java.util.UUID.randomUUID().toString(),
                     name = name,
                     confidence = 1.0,
                     dealAvailable = true
                 )
             }
             // Merge, avoiding duplicates if any logic needed? 
             // For now just set initial.
             shoppingListItems = convertedItems
        }
    }

    LaunchedEffect(searchText.text) {
        if (searchText.text.length >= 2) {
            isSearching = true
            try {
                // Parity: Use real API for keywords
                kotlinx.coroutines.delay(300) // Debounce
                val results = apiService.searchKeywords(searchText.text)
                suggestions = results
            } catch (e: Exception) {
                // Fallback / Empty
                suggestions = emptyList()
            } finally {
                isSearching = false
            }
        } else {
            suggestions = emptyList()
        }
    }

    fun addItem(name: String) {
        if (name.isBlank()) return
        if (shoppingListItems.any { it.name.equals(name, ignoreCase = true) }) return
        
        val newItem = DetectedItem(
            id = UUID.randomUUID().toString(),
            name = name,
            confidence = 1.0,
            dealAvailable = true
        )
        val newList = shoppingListItems + newItem
        shoppingListItems = newList
        sessionManager.saveShoppingList(newList.map { it.name }) // Sync persistence
        
        searchText = androidx.compose.ui.text.input.TextFieldValue("")
        suggestions = emptyList()
    }

    Scaffold(
        containerColor = Color(0xFFF2F2F7) // systemGroupedBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Column(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp, bottom = 10.dp)
                ) {
                    Text(stringResource(app.serfeli.R.string.scan_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(app.serfeli.R.string.scan_subtitle), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }

                // Search Bar Section
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .zIndex(2f)
                ) {
                    Column {
                        // Search Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(12.dp)) // iOS 12-16dp
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                                .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.05f), ambientColor = Color.Transparent),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Search, null, tint = Color.Gray.copy(alpha = 0.8f))
                            Spacer(modifier = Modifier.width(12.dp))
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchText,
                                onValueChange = { searchText = it },
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                    onDone = { addItem(searchText.text) }
                                ),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (searchText.text.isEmpty()) {
                                            Text(stringResource(app.serfeli.R.string.scan_search_hint), color = Color.Gray.copy(alpha = 0.6f))
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                            if (searchText.text.isNotEmpty()) {
                                Button(
                                    onClick = { addItem(searchText.text) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), // slightly more padding
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(stringResource(app.serfeli.R.string.add_button), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Suggestions List
                        AnimatedVisibility(visible = suggestions.isNotEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                shadowElevation = 10.dp, // Higher elevation to "float"
                                color = Color.White
                            ) {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                   suggestions.take(5).forEach { suggestion ->
                                       Row(
                                           modifier = Modifier
                                               .fillMaxWidth()
                                               .clickable { 
                                                   searchText = androidx.compose.ui.text.input.TextFieldValue(
                                                       text = suggestion,
                                                       selection = androidx.compose.ui.text.TextRange(suggestion.length)
                                                   )
                                                   suggestions = emptyList()
                                               }
                                               .padding(horizontal = 16.dp, vertical = 14.dp),
                                           verticalAlignment = Alignment.CenterVertically
                                       ) {
                                           Icon(Icons.Filled.Search, null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                           Spacer(modifier = Modifier.width(12.dp))
                                           Text(suggestion, modifier = Modifier.weight(1f), fontSize = 16.sp)
                                           Icon(Icons.Filled.ArrowForward, null, tint = Color.Gray, modifier = Modifier.size(12.dp)) // arrow.up.left equivalent
                                       }
                                       if (suggestion != suggestions.take(5).last()) {
                                           Divider(modifier = Modifier.padding(start = 44.dp), color = Color.Gray.copy(alpha = 0.1f))
                                       }
                                   }
                               }
                            }
                        }
                    }
                }

                // List Content
                if (shoppingListItems.isEmpty()) {
                     Column(
                         modifier = Modifier.fillMaxSize(),
                         horizontalAlignment = Alignment.CenterHorizontally,
                         verticalArrangement = Arrangement.Center
                     ) {

                         Icon(
                             Icons.Filled.AddShoppingCart, 
                             null, 
                             modifier = Modifier.size(60.dp), 
                             tint = Color.Gray.copy(alpha = 0.2f)
                         )
                         Spacer(modifier = Modifier.height(16.dp))
                         Text(stringResource(app.serfeli.R.string.scan_empty_title), style = MaterialTheme.typography.titleMedium, color = Color.Gray, fontWeight = FontWeight.Bold)
                         Text(stringResource(app.serfeli.R.string.scan_empty_subtitle), style = MaterialTheme.typography.bodyMedium, color = Color.Gray.copy(alpha = 0.8f))
                         Spacer(modifier = Modifier.height(100.dp)) // Offset visual center
                     }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp, start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp), // Slightly tighter
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(shoppingListItems, key = { it.id }) { item ->
                            ItemRow(item = item, onRemove = {
                                val newList = shoppingListItems.filter { it.id != item.id }
                                shoppingListItems = newList
                                // Update SessionManager to keep in sync
                                sessionManager.saveShoppingList(newList.map { it.name })
                            })
                        }
                    }
                }
            }

            // Action Button (Floating)
            if (shoppingListItems.isNotEmpty()) {
                var isSubmitting by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Button(
                        onClick = {
                            if (isSubmitting) return@Button
                            isSubmitting = true
                            scope.launch {
                                try {
                                    val scanId = UUID.randomUUID().toString()
                                    sessionManager.saveLastScanId(scanId) // Ensure we save it for BrandSelection
                                    apiService.confirmScan(scanId, shoppingListItems)
                                    onNavigateToDeals()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    // Show error, do not navigate if scan failed (backend won't have data)
                                    // For now, we can reset isSubmitting to allow retry
                                    // ideally show a Snackbar
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = Primary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(14.dp), // Match iOS often using 12-14 for bottoms
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(stringResource(app.serfeli.R.string.find_best_deals), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Filled.ArrowForward, null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemRow(item: DetectedItem, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.05f)) // Very subtle shadow
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.ShoppingCart, null, tint = Primary, modifier = Modifier.size(20.dp))
        }
        
        Spacer(modifier = Modifier.width(14.dp))
        
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Delete, null, tint = Color.Red.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        }
    }
}
