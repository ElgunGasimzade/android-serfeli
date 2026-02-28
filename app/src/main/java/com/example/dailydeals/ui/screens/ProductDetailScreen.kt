package com.example.dailydeals.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.dailydeals.data.RouteCacheService
import kotlinx.coroutines.launch
import com.example.dailydeals.model.Product
import com.example.dailydeals.ui.theme.Primary
import com.example.dailydeals.R
import androidx.compose.ui.res.stringResource
import com.example.dailydeals.ui.components.MapActionSheet

// Simple static holder to pass data without serialization or new API endpoints
object LocalProductStore {
    var selected: Product? = null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isAdding by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Get product from local store
    val product = remember { LocalProductStore.selected }
    
    // Map Sheet State
    var showMapSheet by remember { mutableStateOf(false) }
    val mapSheetState = rememberModalBottomSheetState()

    if (product != null) {
        val p = product

        Scaffold(
            topBar = {
                // iOS-like Navbar: Transparent with back button, no title
                Row(
                     modifier = Modifier
                         .fillMaxWidth()
                         .padding(top = 8.dp, start = 8.dp), // Safe area handled by system bars usually, but adding buffer
                     verticalAlignment = Alignment.CenterVertically
                ) {
                     IconButton(
                         onClick = onNavigateBack,
                         modifier = Modifier
                             .padding(8.dp)
                             .background(Color.White.copy(alpha = 0.9f), CircleShape)
                             .shadow(2.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.1f))
                             .size(32.dp) // iOS Back Button size
                     ) {
                         Icon(
                            Icons.Filled.ArrowBack, 
                            contentDescription = androidx.compose.ui.res.stringResource(com.example.dailydeals.R.string.back), 
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                         )
                     }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            // "Add to List" Button pinned to bottom, but ABOVE the BottomNavigation (which is handled by Main Activity)
            // We use bottomBar here effectively to pin it.
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.95f))
                        .shadow(elevation = 10.dp, spotColor = Color.Black.copy(alpha = 0.05f))
                        .padding(20.dp), // Symmetric 20.dp padding ensures visual centering
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { 
                            if (!isAdding) {
                                isAdding = true
                                scope.launch {
                                    try {
                                        val routeService = RouteCacheService(context)
                                        val planId = routeService.addItemToActivePlan(p)
                                        
                                        isAdding = false // Stop spinner immediately upon success/fail

                                        if (planId != null) {
                                            snackbarHostState.showSnackbar(context.getString(com.example.dailydeals.R.string.added_to_plan))
                                        } else {
                                            snackbarHostState.showSnackbar(context.getString(com.example.dailydeals.R.string.add_failed))
                                        }
                                    } catch (e: Exception) {
                                        isAdding = false // Ensure spinner stops on error
                                        snackbarHostState.showSnackbar("Unable to connect. Please try again.")
                                    } finally {
                                        isAdding = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp) // iOS Large Button Height
                            .shadow(4.dp, RoundedCornerShape(14.dp), spotColor = Color(0xFF007AFF).copy(alpha = 0.3f)), // iOS corner radius
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF007AFF) 
                        )
                    ) {
                        if (isAdding) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                        } else {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(androidx.compose.ui.res.stringResource(com.example.dailydeals.R.string.add_to_list), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            },
            containerColor = Color.White 
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 140.dp) // Extra padding for bottom bar + pin button
            ) {
                // Extended Hero Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp) // iOS image height
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (p.imageUrl.isNotEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(p.imageUrl),
                            contentDescription = p.name,
                            contentScale = ContentScale.Fit, // Contain
                            modifier = Modifier
                                .size(280.dp) 
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = Color(0xFFE5E7EB),
                            modifier = Modifier.size(100.dp)
                        )
                    }
                    
                     // Discount Badge (Top Right over image)
                     if ((p.discountPercent ?: 0) > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 100.dp, end = 20.dp) // Positioned relative to image area
                                .background(Color(0xFFFF3B30), RoundedCornerShape(20.dp)) // Red Pill
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.off_percent_format, p.discountPercent ?: 0),
                                color = Color.White, 
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // Content Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White) // iOS white background, not gray
                        .padding(16.dp) // iOS standard padding
                ) {
                    // Store Info Row
                    // Store Info Row (iOS Style - Single Link)
                    // Store Info Row (iOS Style - Brand + Store Link)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top, // Align top so wrapping text doesn't center-align weirdly
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Brand Name (Left)
                        p.brand?.let { brand ->
                            Text(
                                text = brand.uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, letterSpacing = 1.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8E8E93), 
                                modifier = Modifier
                                    .padding(top = 6.dp) // Align visually with store icon/text
                                    .weight(1f, fill = false) // Don't hog space if short
                                    .padding(end = 8.dp)
                            )
                        }
                        
                        // Store Link (Right)
                        p.store?.let { store ->
                            Row(
                                modifier = Modifier
                                    .clickable { showMapSheet = true }
                                    .padding(vertical = 4.dp)
                                    .weight(1f, fill = false), // Allow wrapping, don't force full expansion but take what's needed
                                verticalAlignment = Alignment.Top
                            ) {
                                // Spacer to push to right if brand is missing or short? 
                                // Actually SpaceBetween handles that. 
                                // But if brand is present, we want this to be on the right.
                                
                                Icon(
                                    imageVector = Icons.Default.Place, 
                                    contentDescription = null,
                                    tint = Color(0xFF007AFF), 
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = store,
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp), // Smaller as requested
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF007AFF), 
                                    textDecoration = TextDecoration.Underline,
                                    maxLines = 2, 
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.End // iOS style often right aligns this
                                )
                            }
                        }
                    }
                }
                
                // White Body for Title/Price
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(20.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp)) // Added spacing
                    Text(
                        text = p.name,
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp, lineHeight = 30.sp), // Slightly larger
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        // No maxLines to allow full name
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Price
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = stringResource(R.string.currency_format, p.price),
                            style = MaterialTheme.typography.displayMedium.copy(fontSize = 34.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF007AFF), 
                            letterSpacing = (-1).sp
                        )
                        
                        p.originalPrice?.let {
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.currency_format, it),
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                textDecoration = TextDecoration.LineThrough,
                                color = Color(0xFFAEAEB2), 
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))


                }
            }
        }
        
        // Use the new MapActionSheet
        if (showMapSheet && p.store != null) {
            val storeInfo = com.example.dailydeals.model.MockData.stores.find { it.name.equals(p.store, ignoreCase = true) }
            
            com.example.dailydeals.ui.components.MapActionSheet(
                storeName = p.store!!,
                lat = storeInfo?.lat,
                lon = storeInfo?.lon,
                onDismissRequest = { showMapSheet = false },
                sheetState = mapSheetState
            )
        }
    }
}



