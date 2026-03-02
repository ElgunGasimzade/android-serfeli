package app.serfeli.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.serfeli.ui.utils.MapUtils
import app.serfeli.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapActionSheet(
    storeName: String,
    lat: Double? = null,
    lon: Double? = null,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    val context = LocalContext.current
    
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.White, // iOS Sheets are white/blur
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color(0xFFC7C7CC))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 50.dp) // Safety padding + navigation bar area
        ) {
            Text(
                text = stringResource(R.string.nav_navigate_to, storeName),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                color = Color(0xFF8E8E93),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 12.dp)
            )
            
            Divider(color = Color(0xFFE5E5EA))
            
            // Google Maps
            MapOptionRole(
                text = stringResource(R.string.nav_google_maps),
                icon = Icons.Default.Place, // Replace with specific logo if available
                onClick = {
                    if (lat != null && lon != null) {
                        MapUtils.openMap(context, lat, lon, storeName, "Google Maps")
                    } else {
                        MapUtils.searchMap(context, "Google Maps", storeName)
                    }
                    onDismissRequest()
                }
            )
            
            Divider(color = Color(0xFFE5E5EA), modifier = Modifier.padding(start = 56.dp))
            
            // Waze
            MapOptionRole(
                text = stringResource(R.string.nav_waze),
                icon = Icons.Default.Navigation, // Replace with specific logo if available
                onClick = {
                    if (lat != null && lon != null) {
                        MapUtils.openMap(context, lat, lon, storeName, "Waze")
                    } else {
                        MapUtils.searchMap(context, "Waze", storeName)
                    }
                    onDismissRequest()
                }
            )
            
            Divider(color = Color(0xFFE5E5EA), modifier = Modifier.padding(start = 56.dp))
            
            // Default Maps
            MapOptionRole(
                text = stringResource(R.string.nav_open_maps),
                icon = Icons.Default.Map,
                onClick = {
                    if (lat != null && lon != null) {
                        MapUtils.openMap(context, lat, lon, storeName, null)
                    } else {
                        MapUtils.searchMap(context, "", storeName)
                    }
                    onDismissRequest()
                }
            )
        }
    }
}

@Composable
fun MapOptionRole(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF007AFF), // iOS Blue
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
            color = Color.Black
        )
    }
}
