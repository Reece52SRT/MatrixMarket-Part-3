package com.matrixmarket.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.matrixmarket.app.R
import com.matrixmarket.app.ui.theme.RoyalPurple

// The 5-tab layout from the Part 1 mockups. Labels are string resources so the
// Settings > Language switcher translates them too.
enum class BottomNavItem(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {
    Profile("profile", R.string.nav_profile, Icons.Filled.Person),
    Meet("meet", R.string.nav_meet, Icons.Filled.Event),
    Sell("create_listing", R.string.nav_sell, Icons.Filled.AddCircle),
    Listings("listings", R.string.nav_listings, Icons.Filled.Storefront),
    Settings("settings", R.string.nav_settings, Icons.Filled.Settings)
}

@Composable
fun MatrixBottomNavBar(selected: BottomNavItem?, onNavigate: (BottomNavItem) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        BottomNavItem.entries.forEach { item ->
            val label = stringResource(item.labelRes)
            NavigationBarItem(
                selected = selected == item,
                onClick = { onNavigate(item) },
                icon = { Icon(item.icon, contentDescription = label) },
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = RoyalPurple,
                    selectedTextColor = RoyalPurple,
                    indicatorColor = RoyalPurple.copy(alpha = 0.12f)
                )
            )
        }
    }
}
