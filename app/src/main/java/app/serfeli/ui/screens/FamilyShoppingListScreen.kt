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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.serfeli.R
import app.serfeli.data.RetrofitClient
import app.serfeli.data.SessionManager
import app.serfeli.model.FamilyShoppingItem
import app.serfeli.model.UpdateFamilyItemRequest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyShoppingListScreen(
    familyId: String,
    listId: Int,
    familyName: String,
    sessionManager: SessionManager,
    onNavigateBack: () -> Unit,
    onNavigateToAddItems: (String, Int) -> Unit,
    onNavigateToShoppingPlan: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val apiService = remember { RetrofitClient.apiService }
    val userId = sessionManager.getUserId() ?: ""
    
    var items by remember { mutableStateOf<List<FamilyShoppingItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val pendingCount = items.count { it.status == "pending" }
    val purchasedCount = items.count { it.status == "purchased" }
    val totalPendingCost = items.filter { it.status == "pending" }.sumOf { (it.price ?: 0.0) * it.quantity }
    
    fun startShopping() {
        val pendingItems = items.filter { it.status == "pending" }
        if (pendingItems.isEmpty()) return
        
        val ids = pendingItems.mapNotNull { it.productId }
        val genericNames = pendingItems.filter { it.productId == null }.map { it.itemName }
        
        sessionManager.saveSelectedIds(ids)
        sessionManager.saveShoppingList(genericNames)
        
        // This is a bit of a hack to navigate to a bottom nav bar tab from within a screen,
        // but normally we can just navigate("shopping_plan")
        // We'll pass an extra callback to do it properly.
    }
    
    fun loadItems() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = apiService.getShoppingList(familyId, listId)
                items = response.items
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Unable to connect. Please try again."
            } finally {
                isLoading = false
            }
        }
    }
    
    LaunchedEffect(familyId, listId) {
        loadItems()
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
                    IconButton(onClick = { onNavigateToAddItems(familyId, listId) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Items", tint = Color(0xFF007AFF))
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && items.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (items.isEmpty()) {
                EmptyShoppingListView(onAddClick = { onNavigateToAddItems(familyId, listId) })
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Stats
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${items.size} " + stringResource(R.string.items),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "$pendingCount ${stringResource(R.string.pending)} • $purchasedCount ${stringResource(R.string.purchased)}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            
                            if (totalPendingCost > 0) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = stringResource(R.string.total_estimate),
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.2f ₼", totalPendingCost),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF007AFF) // Blue
                                    )
                                }
                            }
                        }
                    }
                    
                    // The list body
                    items(items, key = { it.id }) { item ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                    scope.launch {
                                        try {
                                            apiService.deleteShoppingListItem(item.id, userId)
                                            loadItems()
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

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                val color = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) Color.Red else Color.Transparent
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                                    }
                                }
                            }
                        ) {
                            FamilyShoppingItemCard(
                                item = item,
                                onToggleStatus = {
                                    scope.launch {
                                        try {
                                            val newStatus = if (item.status == "pending") "purchased" else "pending"
                                            val copy = items.toMutableList()
                                            val index = copy.indexOfFirst { it.id == item.id }
                                            if (index != -1) {
                                                copy[index] = item.copy(status = newStatus)
                                                items = copy
                                            }
                                            apiService.updateShoppingListItem(item.id, UpdateFamilyItemRequest(userId = userId, status = newStatus))
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            loadItems()
                                        }
                                    }
                                },
                                onIncrement = {
                                    scope.launch {
                                        try {
                                            val newQuantity = item.quantity + 1
                                            val copy = items.toMutableList()
                                            val index = copy.indexOfFirst { it.id == item.id }
                                            if (index != -1) {
                                                copy[index] = item.copy(quantity = newQuantity)
                                                items = copy
                                            }
                                            apiService.updateShoppingListItem(item.id, UpdateFamilyItemRequest(userId = userId, quantity = newQuantity))
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                },
                                onDecrement = {
                                    if (item.quantity > 1) {
                                        scope.launch {
                                            try {
                                                val newQuantity = item.quantity - 1
                                                val copy = items.toMutableList()
                                                val index = copy.indexOfFirst { it.id == item.id }
                                                if (index != -1) {
                                                    copy[index] = item.copy(quantity = newQuantity)
                                                    items = copy
                                                }
                                                apiService.updateShoppingListItem(item.id, UpdateFamilyItemRequest(userId = userId, quantity = newQuantity))
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyShoppingListView(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = null,
            tint = Color.Gray.copy(alpha = 0.5f),
            modifier = Modifier.size(60.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.no_items_yet),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.add_items_to_start),
            fontSize = 15.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onAddClick,
            modifier = Modifier
                .height(56.dp)
                .widthIn(min = 200.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.add_items), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun FamilyShoppingItemCard(
    item: FamilyShoppingItem,
    onToggleStatus: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val isPurchased = item.status == "purchased"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            IconButton(onClick = onToggleStatus, modifier = Modifier.size(28.dp)) {
                if (isPurchased) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Purchased", tint = Color(0xFF34C759), modifier = Modifier.size(28.dp))
                } else {
                    Icon(Icons.Outlined.Circle, contentDescription = "Pending", tint = Color.Gray, modifier = Modifier.size(28.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Info Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.itemName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPurchased) Color.Gray else Color.Black,
                    textDecoration = if (isPurchased) TextDecoration.LineThrough else TextDecoration.None
                )
                
                if (!item.brandName.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.brandName,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                
                if (!item.storeName.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ShoppingCart,
                            contentDescription = "Store",
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.storeName,
                            fontSize = 12.sp,
                            color = Color(0xFF007AFF) // Match iOS blue
                        )
                    }
                }
                
                if (item.price != null && item.price > 0.0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "%.2f ₼ /ea", item.price),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPurchased) Color.Gray else Color(0xFF00C853)
                    )
                }
                
                if (!item.notes.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Note: ${item.notes}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.added_by, item.addedBy.username),
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
            }
            
            // Stepper (Quantity)
            if (!isPurchased) {
                Row(
                    modifier = Modifier.background(Color(0xFFF2F2F7), RoundedCornerShape(8.dp)),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDecrement, modifier = Modifier.defaultMinSize(minWidth = 32.dp), contentPadding = PaddingValues(0.dp)) {
                        Text("-", fontSize = 18.sp, color = Color.Black)
                    }
                    Text("${item.quantity}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(horizontal = 4.dp))
                    TextButton(onClick = onIncrement, modifier = Modifier.defaultMinSize(minWidth = 32.dp), contentPadding = PaddingValues(0.dp)) {
                        Text("+", fontSize = 18.sp, color = Color.Black)
                    }
                }
            } else {
                Text("x${item.quantity}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            }
        }
    }
}
