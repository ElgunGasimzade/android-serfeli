package app.serfeli.model

import java.util.UUID

data class ShoppingItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val location: String,
    val price: Double,
    val savings: Double,
    var isChecked: Boolean = false
)

data class Stop(
    val id: String = UUID.randomUUID().toString(),
    val sequenceNumber: Int,
    val storeName: String,
    val distanceAway: String,
    val itemCount: Int,
    val colorHex: Long,
    val items: List<ShoppingItem>
)
