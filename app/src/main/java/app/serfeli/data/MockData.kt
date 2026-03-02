package app.serfeli.model

import java.util.UUID

object MockData {
    private fun makeProduct(
        name: String, 
        price: Double, 
        originalPrice: Double?, 
        discount: Int?, 
        store: String,
        image: String = ""
    ): Product {
        return Product(
            id = UUID.randomUUID().toString(),
            name = name,
            price = price,
            originalPrice = originalPrice,
            discountPercent = discount,
            store = store,
            imageUrl = image,
            brand = "Generic",
            category = "Groceries",

        )
    }

    val stores = listOf(
        Store("Bravo", 40.4093, 49.8671),
        Store("Araz", 40.4093, 49.8671),
        Store("Grandmart", 40.4093, 49.8671),
        Store("Bolmart", 40.4093, 49.8671)
    )

    val heroProduct = Hero(
        title = "Super Saver Deal",
        subtitle = "Limited Time Offer",
        product = makeProduct("Premium Coffee Beans", 12.50, 25.00, 50, "Bravo", "https://i.imgur.com/7v8i9pI.png")
    )

    val products = listOf(
        makeProduct("Ariel Detergent 5kg", 18.99, 24.99, 24, "Bravo", "https://i.imgur.com/5y3y7pI.png"),
        makeProduct("Coca-Cola 2L", 2.50, 3.20, 22, "Araz", "https://i.imgur.com/9y3y7pI.png"),
        makeProduct("Lays Chips Classic", 3.40, 4.50, 24, "Rahat", "https://i.imgur.com/2y3y7pI.png"),
        makeProduct("Colgate Toothpaste", 4.20, 6.00, 30, "Grandmart", "https://i.imgur.com/4y3y7pI.png"),
        makeProduct("Pampers Diapers", 22.00, 28.00, 21, "Bolmart", "https://i.imgur.com/8y3y7pI.png"),
        makeProduct("Fairy Dish Soap", 3.80, 5.50, 31, "Bazarstore", "https://i.imgur.com/1y3y7pI.png"),
        makeProduct("Nutella 750g", 8.50, 11.20, 24, "Bravo", "https://i.imgur.com/3y3y7pI.png"),
        makeProduct("Persil Gel", 15.60, 20.00, 22, "Araz", "https://i.imgur.com/6y3y7pI.png")
    )

    val homeFeed = HomeFeedResponse(
        hero = heroProduct,
        categories = emptyList(),
        products = products
    )
}
