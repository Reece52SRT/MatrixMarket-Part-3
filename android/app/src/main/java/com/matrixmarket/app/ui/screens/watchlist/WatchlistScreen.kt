package com.matrixmarket.app.ui.screens.watchlist

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matrixmarket.app.R
import com.matrixmarket.app.di.AppContainer
import com.matrixmarket.app.ui.components.EmptyBox
import com.matrixmarket.app.ui.components.ErrorBox
import com.matrixmarket.app.ui.components.ListingImage
import com.matrixmarket.app.ui.components.LoadingBox
import com.matrixmarket.app.ui.components.Pill
import com.matrixmarket.app.ui.components.displayText
import com.matrixmarket.app.ui.theme.ErrorRed
import com.matrixmarket.app.ui.theme.LavenderLight
import com.matrixmarket.app.ui.theme.RoyalPurple
import com.matrixmarket.app.ui.theme.listingTypeColor
import com.matrixmarket.app.util.DateFormats
import com.matrixmarket.app.util.Resource
import com.matrixmarket.app.util.ViewModelFactory

@Composable
fun WatchlistScreen(onBack: () -> Unit, onListingClick: (Int) -> Unit) {
    val viewModel: WatchlistViewModel = viewModel(factory = ViewModelFactory { WatchlistViewModel(AppContainer.watchlistRepository) })
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Surface(color = LavenderLight, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                Text(stringResource(R.string.saved_listings), style = MaterialTheme.typography.headlineSmall)
            }

            when (val s = state) {
                is Resource.Loading, Resource.Idle -> LoadingBox()
                is Resource.Error -> ErrorBox(s.displayText(), onRetry = { viewModel.load() })
                is Resource.Success -> {
                    if (s.data.isEmpty()) {
                        EmptyBox(stringResource(R.string.no_saved_listings), stringResource(R.string.no_saved_listings_desc))
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
                            items(s.data, key = { it.listingId }) { listing ->
                                val color = listingTypeColor(listing.listingType)
                                val available = listing.status == "Active"
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().clickable { onListingClick(listing.listingId) }
                                ) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        ListingImage(
                                            listing.imageUrl, listing.categoryName.take(2).uppercase(), color,
                                            RoundedCornerShape(10.dp), Modifier.size(56.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(listing.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(DateFormats.formatRand(listing.price), color = RoyalPurple)
                                            if (!available) {
                                                Spacer(Modifier.height(2.dp))
                                                Pill(stringResource(R.string.no_longer_available), ErrorRed)
                                            }
                                        }
                                        IconButton(onClick = { viewModel.remove(listing.listingId) }) {
                                            Icon(Icons.Filled.Favorite, contentDescription = stringResource(R.string.remove_from_saved), tint = RoyalPurple)
                                        }
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
