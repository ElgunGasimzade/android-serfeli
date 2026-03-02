package app.serfeli.ui.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

object MapUtils {

    fun openMap(context: Context, lat: Double, lon: Double, label: String? = null, appName: String? = null) {
        val labelEncoded = Uri.encode(label ?: "Store")
        // Base URI
        // Google Maps supports: "google.navigation:q=lat,lng" for nav, or "geo:lat,lng?q=lat,lng(Label)" for viewing
        // Waze supports: "waze://?ll=lat,lng&navigate=yes"
        
        val intent = Intent(Intent.ACTION_VIEW)
        
        if (appName == "Waze") {
            val wazeUri = "waze://?ll=$lat,$lon&navigate=yes"
            intent.data = Uri.parse(wazeUri)
            intent.setPackage("com.waze")
        } else if (appName == "Google Maps") {
            // Use google.navigation for direct turn-by-turn if desired, or geo for marker
            // User requested "directly create route" -> Navigation
            val gmMapsUri = "google.navigation:q=$lat,$lon"
            intent.data = Uri.parse(gmMapsUri)
            intent.setPackage("com.google.android.apps.maps")
        } else {
            // Generic geo intent
            val uri = "geo:$lat,$lon?q=$lat,$lon($labelEncoded)"
            intent.data = Uri.parse(uri)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback for Waze/Maps if specific app not installed: try generic geo or browser
             if (appName != null) {
                 // Try generic if specific failed
                 openMap(context, lat, lon, label, null) 
             } else {
                 val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lon"))
                 context.startActivity(fallbackIntent)
             }
        }
    }

    fun searchMap(context: Context, appName: String, query: String) {
        val queryEncoded = Uri.encode(query)
        val uri = "geo:0,0?q=$queryEncoded"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        
        when (appName) {
            "Google Maps" -> intent.setPackage("com.google.android.apps.maps")
            "Waze" -> intent.setPackage("com.waze")
            else -> { /* Let system decide or try Google Maps first */
                intent.setPackage("com.google.android.apps.maps")
            }
        }

         if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback: If specific app not found, try generic
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
             if (fallbackIntent.resolveActivity(context.packageManager) != null) {
                 context.startActivity(fallbackIntent)
             } else {
                 // Open in browser
                 val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$queryEncoded"))
                 context.startActivity(browserIntent)
             }
        }
    }
}
