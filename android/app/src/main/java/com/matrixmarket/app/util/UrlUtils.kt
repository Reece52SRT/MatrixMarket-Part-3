package com.matrixmarket.app.util

// Listing photos are stored as "/api/v1/media/<name>" - prefix them with the API host.
object UrlUtils {
    fun resolveImageUrl(baseUrl: String, imageUrl: String?): String? {
        if (imageUrl.isNullOrBlank()) return null
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://") ||
            imageUrl.startsWith("file:") || imageUrl.startsWith("content:")
        ) return imageUrl
        return baseUrl.trimEnd('/') + "/" + imageUrl.trimStart('/')
    }
}
