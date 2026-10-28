package com.matrixmarket.app.ui.screens.notifications

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matrixmarket.app.R
import com.matrixmarket.app.data.remote.dto.NotificationResponse
import com.matrixmarket.app.di.AppContainer
import com.matrixmarket.app.ui.components.EmptyBox
import com.matrixmarket.app.ui.components.ErrorBox
import com.matrixmarket.app.ui.components.LoadingBox
import com.matrixmarket.app.ui.components.displayText
import com.matrixmarket.app.ui.theme.LavenderLight
import com.matrixmarket.app.ui.theme.MeetingTeal
import com.matrixmarket.app.ui.theme.RoyalPurple
import com.matrixmarket.app.ui.theme.SuccessGreen
import com.matrixmarket.app.ui.theme.TextMuted
import com.matrixmarket.app.ui.theme.WarningAmber
import com.matrixmarket.app.util.DateFormats
import com.matrixmarket.app.util.Resource
import com.matrixmarket.app.util.ViewModelFactory

@Composable
fun NotificationsScreen(onBack: () -> Unit, onOpenListing: (Int) -> Unit, onOpenMeetings: () -> Unit) {
    val viewModel: NotificationsViewModel = viewModel(
        factory = ViewModelFactory { NotificationsViewModel(AppContainer.notificationRepository) }
    )
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Surface(color = LavenderLight, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                Text(stringResource(R.string.notifications), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                val current = state
                if (current is Resource.Success && current.data.any { !it.isRead }) {
                    TextButton(onClick = { viewModel.markAllRead() }) { Text(stringResource(R.string.mark_all_read)) }
                }
            }

            when (val s = state) {
                is Resource.Loading, Resource.Idle -> LoadingBox()
                is Resource.Error -> ErrorBox(s.displayText(), onRetry = { viewModel.load() })
                is Resource.Success -> {
                    if (s.data.isEmpty()) {
                        EmptyBox(stringResource(R.string.no_notifications), stringResource(R.string.no_notifications_desc))
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            items(s.data, key = { it.id }) { item ->
                                NotificationRow(item) {
                                    viewModel.markRead(item)
                                    when {
                                        item.meetingId != null -> onOpenMeetings()
                                        item.listingId != null -> onOpenListing(item.listingId)
                                    }
                                }
                            }
                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(item: NotificationResponse, onClick: () -> Unit) {
    val (icon, color) = iconFor(item.type)
    Surface(
        color = if (item.isRead) Color.White else RoyalPurple.copy(alpha = 0.08f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(38.dp).background(color.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(item.body, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(2.dp))
                Text(DateFormats.dateTime(item.createdAt), style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            if (!item.isRead) {
                Box(Modifier.padding(top = 6.dp).size(9.dp).background(RoyalPurple, CircleShape))
            }
        }
    }
}

private fun iconFor(type: String): Pair<ImageVector, Color> = when (type) {
    "offer" -> Icons.Filled.LocalOffer to RoyalPurple
    "meeting" -> Icons.Filled.Event to MeetingTeal
    "trade_confirmed" -> Icons.Filled.CheckCircle to SuccessGreen
    "listing_sold" -> Icons.Filled.Sell to WarningAmber
    else -> Icons.Filled.Notifications to TextMuted
}
