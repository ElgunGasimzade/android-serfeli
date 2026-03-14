package app.serfeli.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.ui.draw.shadow
import app.serfeli.data.RetrofitClient
import app.serfeli.data.SessionManager
import app.serfeli.model.NotificationItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationListScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableStateOf(1) }
    var canLoadMore by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }

    fun loadNotifications(reload: Boolean = false) {
        if (!isLoading && !isRefreshing && !reload) return
        if (reload) {
            page = 1
            canLoadMore = true
        }
        if (!canLoadMore) return

        if (reload) isRefreshing = true else isLoading = true
        
        coroutineScope.launch {
            try {
                val userId = sessionManager.getUserId()
                val deviceId = sessionManager.getDeviceId()
                val res = RetrofitClient.apiService.getNotifications(
                    userId = userId,
                    deviceId = deviceId,
                    page = page,
                    limit = 20
                )
                if (reload) {
                    notifications = res.data
                } else {
                    notifications = notifications + res.data
                }
                canLoadMore = res.data.isNotEmpty()
                if (canLoadMore) page++
            } catch (e: Exception) {
                if (reload) errorMessage = e.localizedMessage
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadNotifications(reload = true)
    }

    val backgroundColor = Color(0xFFF5F5F7) // Light gray background
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Custom Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onNavigateBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Back", tint = Color.Black)
            }
            
            Text(
                text = "Notifications",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Box(
                modifier = Modifier
                    .shadow(4.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .clickable {
                        coroutineScope.launch {
                            try {
                                RetrofitClient.apiService.markAllNotificationsAsRead(
                                    sessionManager.getUserId(),
                                    sessionManager.getDeviceId()
                                )
                                notifications = notifications.map { it.copy(isRead = true) }
                            } catch (e: Exception) {}
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Read All", color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (isLoading && notifications.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(errorMessage ?: "Unknown Error", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { loadNotifications(reload = true) }) {
                        Text("Retry")
                    }
                }
            } else if (notifications.isEmpty()) {
                Text(
                    "No notifications yet",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray,
                    fontSize = 18.sp
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(notifications, key = { it.id }) { notif ->
                        NotificationItemWithSwipe(
                            notification = notif,
                            onClick = {
                                if (!notif.isRead) {
                                    coroutineScope.launch {
                                        try {
                                            RetrofitClient.apiService.markNotificationAsRead(notif.id)
                                            notifications = notifications.map { 
                                                if (it.id == notif.id) it.copy(isRead = true) else it 
                                            }
                                        } catch(e:Exception) {}
                                    }
                                }
                            },
                            onDelete = {
                                coroutineScope.launch {
                                    try {
                                        RetrofitClient.apiService.deleteNotification(notif.id)
                                        notifications = notifications.filter { it.id != notif.id }
                                    } catch(e:Exception) {}
                                }
                            }
                        )
                        // Removed Divider and added vertical arrangement spacing

                        // Load more
                        if (notif == notifications.last()) {
                            LaunchedEffect(notif) {
                                loadNotifications()
                            }
                        }
                    }
                    if (isLoading && notifications.isNotEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationItemWithSwipe(
    notification: NotificationItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) Color.Red else Color.Transparent
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        NotificationRow(notification, onClick, onDelete)
    }
}

@Composable
fun NotificationRow(
    notification: NotificationItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    val formattedTime = remember(notification.createdAt) {
        try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date = sdf.parse(notification.createdAt) ?: return@remember "Just now"
            val diff = System.currentTimeMillis() - date.time
            val seconds = diff / 1000
            if (seconds < 60) "Just now"
            else if (seconds < 3600) "${seconds / 60}m ago"
            else if (seconds < 86400) "${seconds / 3600}h ago"
            else "${seconds / 86400}d ago"
        } catch (e: Exception) {
            try {
                val sdf2 = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                sdf2.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val date = sdf2.parse(notification.createdAt) ?: return@remember "Just now"
                val diff = System.currentTimeMillis() - date.time
                val seconds = diff / 1000
                if (seconds < 60) "Just now"
                else if (seconds < 3600) "${seconds / 60}m ago"
                else if (seconds < 86400) "${seconds / 3600}h ago"
                else "${seconds / 86400}d ago"
            } catch(e2: Exception) {
                "Just now"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable { 
                isExpanded = !isExpanded
                onClick() 
            }
            .padding(16.dp)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (notification.isRead) Color.LightGray.copy(alpha=0.5f) else Color.Blue)
                    .padding(top = 5.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = notification.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF141932) // Dark blue-black
                    )
                    Text(
                        text = formattedTime,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.body,
                    fontSize = 15.sp,
                    color = Color(0xFF646E82), // Muted slate-blue
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }
        }
        
        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap to minimize",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
        }
    }
}
