package app.serfeli.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import app.serfeli.model.Product
import app.serfeli.R
import app.serfeli.ui.theme.Primary
import androidx.compose.ui.res.stringResource

@Composable
fun ProductCard(
    product: Product,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp, // iOS "Light shadow" often translates to ~2-4dp in Android
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .background(Color.White, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(10.dp) // iOS padding 10pt
    ) {
        // Image Layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp) // iOS frame height 120pt
                .clip(RoundedCornerShape(12.dp)) // iOS 12pt corner radius
                .background(Color(0xFFF2F2F7)) // iOS SystemGray6
                .padding(4.dp) // Inner padding
                ,
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit // iOS scaledToFit (contain)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        // Name
        Text(
            text = product.name,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 15.sp, // iOS Subheadline 15pt
                lineHeight = 20.sp
            ),
            fontWeight = FontWeight.SemiBold, // Slightly bolder than normal
            maxLines = 2, // iOS "max 2 lines"
            overflow = TextOverflow.Ellipsis,
            color = Color.Black
        )
        
        // Store
        Text(
            text = product.store ?: "",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp // iOS Caption 1 12-13pt
            ),
            color = Color(0xFF8E8E93), // iOS SystemGray (Color.Gray matches close enough, but explicit is better)
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.weight(1f)) // Push price to bottom
        Spacer(modifier = Modifier.height(8.dp))
        
        // Price Row
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Current Price
                Text(
                    text = "%.2f ₼".format(product.price),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp // iOS Body/Headline ~17pt
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                // Old Price (if exists)
                product.originalPrice?.let { original ->
                    Text(
                        text = "%.2f ₼".format(original),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp // iOS Caption 1
                        ),
                        textDecoration = TextDecoration.LineThrough,
                        color = Color(0xFF8E8E93) // iOS SystemGray
                    )
                }
            }
            
            // Discount Badge (Blue pill -27%)
            if (product.discountPercent != null && product.discountPercent > 0) {
                 Text(
                    text = stringResource(R.string.off_percent_short, product.discountPercent),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .background(Primary, RoundedCornerShape(12.dp)) // Blue Pill
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
