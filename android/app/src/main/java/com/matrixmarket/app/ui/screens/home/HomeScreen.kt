package com.matrixmarket.app.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matrixmarket.app.R
import com.matrixmarket.app.data.local.db.PendingListingEntity
import com.matrixmarket.app.data.local.db.SyncState
import com.matrixmarket.app.data.remote.dto.ListingResponse
import com.matrixmarket.app.di.AppContainer
import com.matrixmarket.app.ui.components.EmptyBox
import com.matrixmarket.app.ui.components.ErrorBox
import com.matrixmarket.app.ui.components.ListingImage
import com.matrixmarket.app.ui.components.LoadingBox
import com.matrixmarket.app.ui.components.Pill
import com.matrixmarket.app.ui.components.categoryLabel
import com.matrixmarket.app.ui.components.displayText
import com.matrixmarket.app.ui.components.listingTypeLabel
import com.matrixmarket.app.ui.theme.ErrorRed
import com.matrixmarket.app.ui.theme.LavenderLight
import com.matrixmarket.app.ui.theme.RoyalPurple
import com.matrixmarket.app.ui.theme.TextMuted
import com.matrixmarket.app.ui.theme.WarningAmber
import com.matrixmarket.app.ui.theme.listingTypeColor
import com.matrixmarket.app.util.DateFormats
import com.matrixmarket.app.util.Resource
import com.matrixmarket.app.util.ViewModelFactory

@Composable
fun HomeScreen(
    onListingClick: (Int) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenWatchlist: () -> Unit,
    onCreateListing: () -> Unit
) {
    val viewModel: HomeViewModel = viewModel(
        factory = ViewModelFactory { HomeViewModel(AppContainer.listingRepository, AppContainer.notificationRepository) }
    )
    val listingsState by viewModel.listingsState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val unread by viewModel.unreadCount.collectAsState()
    val pending by viewModel.pendingListings.collectAsState()

    LaunchedEffect(Unit) { viewModel.onScreenShown() }

    Surface(color = LavenderLight, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.current_listings),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.loadListings() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh))
                }
                IconButton(onClick = onOpenWatchlist) {
                    Icon(Icons.Filled.Favorite, contentDescription = stringResource(R.string.saved_listings), tint = RoyalPurple)
                }
                IconButton(onClick = onOpenNotifications) {
                    BadgedBox(badge = { if (unread > 0) Badge { Text(if (unread > 99) "99+" else unread.toString()) } }) {
                        Icon(Icons.Filled.Notifications, contentDescription = stringResource(R.string.notifications))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { viewModel.onCategorySelected(null) },
                        label = { Text(stringResource(R.string.all)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalPurple, selectedLabelColor = Color.White)
                    )
                }
                items(categories, key = { it.categoryId }) { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.categoryId,
                        onClick = { viewModel.onCategorySelected(category.categoryId) },
                        label = { Text(categoryLabel(category.categoryId, category.categoryName)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalPurple, selectedLabelColor = Color.White)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            when (val state = listingsState) {
                is Resource.Loading, Resource.Idle -> LoadingBox()
                is Resource.Error -> ErrorBox(message = state.displayText(), onRetry = { viewModel.loadListings() })
                is Resource.Success -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (state.fromCache) {
                            item(key = "cache_note") { CacheNote() }
                        }
                        if (pending.isNotEmpty()) {
                            item(key = "pending_header") {
                                Text(stringResource(R.string.waiting_to_sync), style = MaterialTheme.typography.titleMedium)
                            }
                            items(pending, key = { "pending_${it.localId}" }) { item ->
                                PendingListingCard(item, onRetry = { viewModel.retryPending(item) }, onDiscard = { viewModel.discardPending(item) })
                            }
                        }
                        if (state.data.isEmpty()) {
                            item(key = "empty") {
                                Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    EmptyBox(
                                        title = stringResource(if (state.fromCache) R.string.no_cached_listings else R.string.no_listings),
                                        subtitle = stringResource(R.string.no_listings_desc),
                                        modifier = Modifier.height(220.dp)
                                    )
                                    Button(onClick = onCreateListing, colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple)) {
                                        Text(stringResource(R.string.create_new_listing))
                                    }
                                }
                            }
                        } else {
                            items(state.data, key = { it.listingId }) { listing ->
                                ListingCard(listing = listing, onClick = { onListingClick(listing.listingId) })
                            }
                        }
                        item(key = "bottom_space") { Spacer(Modifier.height(12.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CacheNote() {
    Surface(color = WarningAmber.copy(alpha = 0.18f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CloudOff, contentDescription = null, tint = WarningAmber)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.showing_cached), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ListingCard(listing: ListingResponse, onClick: () -> Unit) {
    val typeColor = listingTypeColor(listing.listingType)
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ListingImage(
                imageUrl = listing.imageUrl,
                placeholderText = listing.categoryName.take(2).uppercase(),
                accent = typeColor,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        listing.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    Pill(listingTypeLabel(listing.listingType), typeColor)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "${DateFormats.formatRand(listing.price)} · ${categoryLabel(listing.categoryId, listing.categoryName)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RoyalPurple,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        stringResource(R.string.seller_score_short, listing.sellerName, listing.sellerKarma),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingListingCard(item: PendingListingEntity, onRetry: () -> Unit, onDiscard: () -> Unit) {
    val failed = item.syncState == SyncState.FAILED
    Surface(
        color = if (failed) ErrorRed.copy(alpha = 0.08f) else WarningAmber.copy(alpha = 0.12f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (failed) Icons.Filled.CloudOff else Icons.Filled.CloudUpload,
                    contentDescription = null,
                    tint = if (failed) ErrorRed else WarningAmber
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${DateFormats.formatRand(item.price)} · ${categoryLabel(item.categoryId, item.categoryName)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                Pill(
                    stringResource(if (failed) R.string.sync_failed else R.string.sync_pending),
                    if (failed) ErrorRed else WarningAmber
                )
            }
            if (failed) {
                item.lastError?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                }
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDiscard) { Text(stringResource(R.string.discard)) }
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                }
            }
        }
    }
}
