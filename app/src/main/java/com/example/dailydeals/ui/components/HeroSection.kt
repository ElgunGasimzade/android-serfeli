package com.example.dailydeals.ui.components

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.dailydeals.model.Hero
import com.example.dailydeals.R
import com.example.dailydeals.ui.theme.Primary
import androidx.compose.ui.res.stringResource

@Composable
fun HeroSection(hero: Hero, onClick: () -> Unit) { // Added onClick
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // No direct shadow/background on the logic container, the card itself is the content below
            // No direct shadow/background on the logic container, the card itself is the content below
            // Removed padding(bottom = 20.dp) to let Grid handle spacing
    ) {
        // Section Header "Deal of the Day ⚡️"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.deal_of_day_lightning),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.ends_soon_caps),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                color = Color(0xFF8E8E93), // SystemGray
                fontWeight = FontWeight.Medium
            )
        }

        // Featured Product Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.1f))
                .background(Color.White, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .clickable { onClick() } // Added clickable
                .padding(16.dp)
        ) {
            // Image Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White) // White bg for image
                    ,
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = hero.product.imageUrl,
                    contentDescription = hero.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit // Contain
                )

                // Large Discount Badge (Red)
                hero.product.discountPercent?.let { percent ->
                    if (percent > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd) // iOS Top Trailing
                                .padding(8.dp)
                                .background(Color(0xFFFF3B30), RoundedCornerShape(8.dp)) // SystemRed
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.off_percent_format, percent),
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer: Product Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Leading: Store/Date + Name
                Column(modifier = Modifier.weight(1f)) {
                    // Store + Date
                    Text(
                        text = hero.product.store ?: "", // Removed Today suffix
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                        color = Color(0xFF8E8E93), // Gray
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = hero.product.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Trailing: Prices
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "%.2f ₼".format(hero.product.price),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF007AFF) // Blue
                    )
                    hero.product.originalPrice?.let { original ->
                        Text(
                            text = "%.2f ₼".format(original),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            textDecoration = TextDecoration.LineThrough,
                            color = Color(0xFF8E8E93) // Gray
                        )
                    }
                }
            }
        }
    }
}
