package com.matrixmarket.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matrixmarket.app.R
import com.matrixmarket.app.di.AppContainer
import com.matrixmarket.app.ui.components.ErrorBox
import com.matrixmarket.app.ui.components.LoadingBox
import com.matrixmarket.app.ui.components.badgeLabel
import com.matrixmarket.app.ui.components.displayText
import com.matrixmarket.app.ui.components.tierLabel
import com.matrixmarket.app.ui.theme.LavenderCard
import com.matrixmarket.app.ui.theme.LavenderLight
import com.matrixmarket.app.ui.theme.RisingTraderTeal
import com.matrixmarket.app.ui.theme.RoyalPurple
import com.matrixmarket.app.ui.theme.TextMuted
import com.matrixmarket.app.ui.theme.badgeColor
import com.matrixmarket.app.ui.theme.tierColor
import com.matrixmarket.app.util.DateFormats
import com.matrixmarket.app.util.KarmaTiers
import com.matrixmarket.app.util.Resource
import com.matrixmarket.app.util.ViewModelFactory

@Composable
fun ProfileScreen(onOpenWatchlist: () -> Unit, onOpenNotifications: () -> Unit) {
    val viewModel: ProfileViewModel = viewModel(
        factory = ViewModelFactory { ProfileViewModel(AppContainer.profileRepository, AppContainer.sessionManager) }
    )
    val state by viewModel.profileState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadProfile() }

    Surface(color = LavenderLight, modifier = Modifier.fillMaxSize()) {
        when (val s = state) {
            is Resource.Loading, Resource.Idle -> LoadingBox()
            is Resource.Error -> ErrorBox(s.displayText(), onRetry = { viewModel.loadProfile() })
            is Resource.Success -> {
                val profile = s.data
                val tierColor = tierColor(profile.trustTier)
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(64.dp).background(tierColor.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = tierColor, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(profile.fullName, style = MaterialTheme.typography.titleLarge)
                            Text(profile.studentEmail, color = TextMuted, style = MaterialTheme.typography.bodySmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!profile.campus.isNullOrBlank()) {
                                    Icon(Icons.Filled.School, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text(profile.campus, color = TextMuted, style = MaterialTheme.typography.bodySmall)
                                    Spacer(Modifier.width(10.dp))
                                }
                                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    stringResource(R.string.member_since, DateFormats.monthYear(profile.memberSince)),
                                    color = TextMuted, style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(22.dp))
                    // Student Score card - colour changes as the tier rises.
                    Surface(color = Color.White, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.student_score), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.points_value, profile.karmaPoints), color = tierColor, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { KarmaTiers.progress(profile.karmaPoints) },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = tierColor,
                                trackColor = tierColor.copy(alpha = 0.15f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = tierColor.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        tierLabel(profile.trustTier), color = tierColor, fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                val next = KarmaTiers.pointsToNext(profile.karmaPoints)
                                Text(
                                    if (next != null) stringResource(R.string.points_to_go, next.first, tierLabel(next.second))
                                    else stringResource(R.string.top_tier_reached),
                                    color = TextMuted, style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    StatRow(Icons.Filled.CheckCircle, stringResource(R.string.trades_completed), profile.tradesCompleted.toString(), RisingTraderTeal)
                    Spacer(Modifier.height(10.dp))
                    StatRow(Icons.Filled.Storefront, stringResource(R.string.active_listings), profile.listingsCount.toString(), RoyalPurple)

                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.trust_badges), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(ProfileViewModel.badges(profile.badges.orEmpty())) { badge ->
                            val color = if (badge.earned) badgeColor(badge.type) else Color.Gray
                            Surface(
                                color = if (badge.earned) badgeColor(badge.type).copy(alpha = 0.15f) else LavenderCard,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp).widthIn(max = 170.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (badge.earned) Icons.Filled.EmojiEvents else Icons.Filled.Lock,
                                            contentDescription = null, tint = color, modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(badgeLabel(badge.type), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium,
                                            color = if (badge.earned) Color.Black else Color.Gray)
                                    }
                                    if (!badge.earned) {
                                        Spacer(Modifier.height(3.dp))
                                        Text(badgeHint(badge.type), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    LinkRow(Icons.Filled.Favorite, stringResource(R.string.saved_listings), onOpenWatchlist)
                    Spacer(Modifier.height(10.dp))
                    LinkRow(Icons.Filled.Notifications, stringResource(R.string.notifications), onOpenNotifications)
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun badgeHint(type: String): String = when (type) {
    "First Trade" -> stringResource(R.string.badge_first_trade_hint)
    "Top Seller" -> stringResource(R.string.badge_top_seller_hint)
    else -> stringResource(R.string.badge_trusted_trader_hint)
}

@Composable
private fun StatRow(icon: ImageVector, label: String, value: String, color: Color) {
    Surface(color = Color.White, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(Modifier.width(10.dp))
            Text(label, modifier = Modifier.weight(1f))
            Text(value, fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun LinkRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = RoyalPurple)
            Spacer(Modifier.width(10.dp))
            Text(label, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextMuted)
        }
    }
}
