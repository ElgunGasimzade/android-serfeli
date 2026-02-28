package com.example.dailydeals.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PersonAdd
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
import com.example.dailydeals.R
import com.example.dailydeals.data.RetrofitClient
import com.example.dailydeals.data.SessionManager
import com.example.dailydeals.model.FamilyListItem
import com.example.dailydeals.model.CreateFamilyRequest
import com.example.dailydeals.model.JoinFamilyRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyMainScreen(
    onNavigateToLists: (String, String) -> Unit, // passes (familyId, familyName)
    sessionManager: SessionManager
) {
    val scope = rememberCoroutineScope()
    val apiService = remember { RetrofitClient.apiService }
    
    var groups by remember { mutableStateOf<List<FamilyListItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Dialog states
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var createGroupName by remember { mutableStateOf("") }
    var joinInviteCode by remember { mutableStateOf("") }
    
    val userId = sessionManager.getUserId() ?: ""
    
    fun loadGroups() {
        if (userId.isEmpty()) {
            isLoading = false
            return
        }
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = apiService.listFamilies(userId)
                groups = response.families
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Unable to connect. Please try again."
            } finally {
                isLoading = false
            }
        }
    }
    
    LaunchedEffect(Unit) {
        loadGroups()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.group_shopping), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showJoinDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Join")
                    }
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF2F2F7), // iOS Grouped Background
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = Color(0xFFF2F2F7) // SystemGray6 background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && groups.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (groups.isEmpty()) {
                NoGroupsView(
                    onCreateClick = { showCreateDialog = true },
                    onJoinClick = { showJoinDialog = true },
                    errorMessage = errorMessage
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(groups) { group ->
                        GroupCard(
                            group = group,
                            onClick = { onNavigateToLists(group.id, group.name) },
                            onLeave = {
                                // Basic leave functionality to replicate iOS (could be expanded)
                                scope.launch {
                                    try {
                                        apiService.leaveFamily(mapOf("familyId" to group.id, "userId" to userId))
                                        loadGroups()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        )
                    }
                }
            }
            
            // Create Dialog
            if (showCreateDialog) {
                AlertDialog(
                    onDismissRequest = { showCreateDialog = false },
                    title = { Text(stringResource(R.string.create_group)) },
                    text = {
                        OutlinedTextField(
                            value = createGroupName,
                            onValueChange = { createGroupName = it },
                            placeholder = { Text(stringResource(R.string.enter_group_name)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (createGroupName.isNotBlank()) {
                                scope.launch {
                                    try {
                                        // Random invite code for simplicity if backend requires it
                                        apiService.createFamily(CreateFamilyRequest(createGroupName, userId))
                                        showCreateDialog = false
                                        createGroupName = ""
                                        loadGroups()
                                    } catch (e: Exception) {
                                        errorMessage = "Unable to connect. Please try again."
                                        showCreateDialog = false
                                    }
                                }
                            }
                        }) {
                            Text(stringResource(R.string.create_group))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateDialog = false }) { Text(stringResource(R.string.cancel)) }
                    }
                )
            }
            
            // Join Dialog
            if (showJoinDialog) {
                AlertDialog(
                    onDismissRequest = { showJoinDialog = false },
                    title = { Text(stringResource(R.string.join_group)) },
                    text = {
                        OutlinedTextField(
                            value = joinInviteCode,
                            onValueChange = { joinInviteCode = it },
                            placeholder = { Text(stringResource(R.string.enter_invite_code)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (joinInviteCode.isNotBlank()) {
                                scope.launch {
                                    try {
                                        apiService.joinFamily(JoinFamilyRequest(joinInviteCode, userId))
                                        showJoinDialog = false
                                        joinInviteCode = ""
                                        loadGroups()
                                    } catch (e: Exception) {
                                        errorMessage = "Unable to connect. Please try again."
                                        showJoinDialog = false
                                    }
                                }
                            }
                        }) {
                            Text(stringResource(R.string.join_group))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showJoinDialog = false }) { Text(stringResource(R.string.cancel)) }
                    }
                )
            }
        }
    }
}

@Composable
fun NoGroupsView(onCreateClick: () -> Unit, onJoinClick: () -> Unit, errorMessage: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0xFF007AFF).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(50.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.no_groups_yet),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.no_groups_desc),
            fontSize = 15.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onCreateClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
        ) {
            Text(stringResource(R.string.create_group), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onJoinClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF007AFF)),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF007AFF))
        ) {
            Text(stringResource(R.string.join_with_code), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = errorMessage, color = Color.Red, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun GroupCard(group: FamilyListItem, onClick: () -> Unit, onLeave: () -> Unit) {
    var showLeaveDialog by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = group.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                // Small badge for Role
                Box(
                    modifier = Modifier
                        .background(
                            if (group.role == "admin") Color(0xFF007AFF).copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (group.role == "admin") stringResource(R.string.admin) else stringResource(R.string.member),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (group.role == "admin") Color(0xFF007AFF) else Color.DarkGray
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.members_count, group.memberCount), fontSize = 14.sp, color = Color.Gray)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.items_to_buy, group.pendingItemsCount.toString()), fontSize = 14.sp, color = Color.Gray) 
                }
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color(0xFFE5E5EA))
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.invite_code_caps),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = group.inviteCode,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                    Button(
                        onClick = { clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(group.inviteCode)) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF).copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF007AFF), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.copy), color = Color(0xFF007AFF), fontSize = 12.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Shopping Lists
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Gray.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .clickable { onClick() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.List, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(stringResource(R.string.shopping_lists), modifier = Modifier.weight(1f), color = Color.Black)
                        
                        if (group.pendingItemsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF007AFF), androidx.compose.foundation.shape.CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = group.pendingItemsCount.toString(),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                    
                    // Leave Group Action
                    Button(
                        onClick = { showLeaveDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Leave Group",
                                tint = Color.Red,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.leave_group_button),
                                color = Color.Red,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text(stringResource(R.string.leave_group_button)) },
            text = { Text(stringResource(R.string.leave_group_confirm, group.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveDialog = false
                        onLeave()
                    }
                ) {
                    Text(stringResource(R.string.leave_group_button), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
