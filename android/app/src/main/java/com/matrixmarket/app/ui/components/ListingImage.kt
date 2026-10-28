package com.matrixmarket.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import coil.compose.SubcomposeAsyncImage
import com.matrixmarket.app.BuildConfig
import com.matrixmarket.app.util.UrlUtils

// Listing photo loaded from blob storage (via the API), with a coloured
// category placeholder while loading or when the listing has no photo.
@Composable
fun ListingImage(
    imageUrl: String?,
    placeholderText: String,
    accent: Color,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    val url = UrlUtils.resolveImageUrl(BuildConfig.API_BASE_URL, imageUrl)
    val placeholder: @Composable () -> Unit = {
        Surface(color = accent.copy(alpha = 0.15f), shape = shape, modifier = Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center) {
                Text(placeholderText, color = accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    Box(modifier.clip(shape)) {
        if (url == null) {
            placeholder()
        } else {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { placeholder() },
                error = { placeholder() }
            )
        }
    }
}
