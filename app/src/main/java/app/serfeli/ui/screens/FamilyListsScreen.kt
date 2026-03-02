package app.serfeli.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.serfeli.R
import app.serfeli.data.RetrofitClient
import app.serfeli.data.SessionManager
import app.serfeli.model.CreateShoppingListRequest
import app.serfeli.model.ShoppingList
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyListsScreen(
    familyId: String,
    familyName: String,
    sessionManager: SessionManager,
    onNavigateBack: () -> Unit,
    onNavigateToList: (String, Int) -> Unit // (familyId, listId)
) {
    val scope = rememberCoroutineScope()
    val apiService = remember { RetrofitClient.apiService }
    
    var lists by remember { mutableStateOf<List<ShoppingList>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Dialog state
    var showCreateDialog by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }
    
    val userId = sessionManager.getUserId() ?: ""
    
    fun loadLists() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = apiService.getShoppingLists(familyId)
                lists = response.lists
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Unable to connect. Please try again."
            } finally {
                isLoading = false
            }
        }
    }
    
    LaunchedEffect(familyId) {
        loadLists()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(familyName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF007AFF))
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create List", tint = Color(0xFF007AFF))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF2F2F7),
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = Color(0xFFF2F2F7) // SystemGray6 background matching iOS InsetGroupedListStyle
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && lists.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (lists.isEmpty()) {
                NoListsView(onCreateClick = { showCreateDialog = true })
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = lists,
                        key = { it.id }
                    ) { list ->
                        // Swipe to delete wrapper
                        val dismissState = rememberDismissState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == DismissValue.DismissedToStart) {
                                    scope.launch {
                                        try {
                                            apiService.deleteShoppingList(list.id)
                                            loadLists()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    true
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismiss(
                            state = dismissState,
                            background = {
                                val color = when (dismissState.targetValue) {
                                    DismissValue.DismissedToStart -> Color.Red
                                    else -> Color.Transparent
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    if (dismissState.targetValue == DismissValue.DismissedToStart) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.White
                                        )
                                    }
                                }
                            },
                            dismissContent = {
                                ListCard(
                                    list = list,
                                    onClick = { onNavigateToList(familyId, list.id) }
                                )
                            },
                            directions = setOf(DismissDirection.EndToStart)
                        )
                    }
                }
            }
            
            // Create Dialog
            if (showCreateDialog) {
                AlertDialog(
                    onDismissRequest = { showCreateDialog = false },
                    title = { Text(stringResource(R.string.create_new_list)) },
                    text = {
                        OutlinedTextField(
                            value = newListName,
                            onValueChange = { newListName = it },
                            placeholder = { Text(stringResource(R.string.list_name)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (newListName.isNotBlank()) {
                                scope.launch {
                                    try {
                                        apiService.createShoppingList(CreateShoppingListRequest(familyId, userId, newListName))
                                        showCreateDialog = false
                                        newListName = ""
                                        loadLists()
                                    } catch (e: Exception) {
                                        errorMessage = "Unable to connect. Please try again."
                                        showCreateDialog = false
                                    }
                                }
                            }
                        }) {
                            Text(stringResource(R.string.create_new_list)) // Should perhaps just be "Create"
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateDialog = false }) { Text(stringResource(R.string.cancel)) }
                    }
                )
            }
            
            if (errorMessage != null && !showCreateDialog) {
                // simple snackbar or text
            }
        }
    }
}

@Composable
fun NoListsView(onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FormatListBulleted,
            contentDescription = null,
            tint = Color.Gray.copy(alpha = 0.5f),
            modifier = Modifier.size(60.dp)
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = stringResource(R.string.no_lists_yet),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.create_list_desc),
            fontSize = 15.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onCreateClick,
            modifier = Modifier
                .height(56.dp)
                .widthIn(min = 200.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
        ) {
            Text(stringResource(R.string.create_new_list), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun ListCard(list: ShoppingList, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp), // iOS InsetGroupedListStyle row
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = list.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (list.pendingCount > 0) {
                    Text(
                        text = stringResource(R.string.items_needed, list.pendingCount),
                        color = Color(0xFF007AFF), // iOS Blue
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = stringResource(R.string.all_items_purchased),
                        color = Color(0xFF34C759), // iOS Green
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = formatDate(list.createdAt),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        // Simple parser
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val date = format.parse(dateString) ?: return dateString
        val outFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        outFormat.format(date)
    } catch (e: Exception) {
        dateString
    }
}
