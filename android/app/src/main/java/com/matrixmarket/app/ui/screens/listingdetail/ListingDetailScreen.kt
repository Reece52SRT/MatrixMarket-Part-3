package com.matrixmarket.app.ui.screens.listingdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matrixmarket.app.R
import com.matrixmarket.app.data.remote.dto.ListingResponse
import com.matrixmarket.app.di.AppContainer
import com.matrixmarket.app.ui.components.ErrorBox
import com.matrixmarket.app.ui.components.ListingImage
import com.matrixmarket.app.ui.components.LoadingBox
import com.matrixmarket.app.ui.components.Pill
import com.matrixmarket.app.ui.components.asString
import com.matrixmarket.app.ui.components.categoryLabel
import com.matrixmarket.app.ui.components.displayText
import com.matrixmarket.app.ui.components.listingTypeLabel
import com.matrixmarket.app.ui.theme.ErrorRed
import com.matrixmarket.app.ui.theme.LavenderCard
import com.matrixmarket.app.ui.theme.LavenderLight
import com.matrixmarket.app.ui.theme.RoyalPurple
import com.matrixmarket.app.ui.theme.TextMuted
import com.matrixmarket.app.ui.theme.WarningAmber
import com.matrixmarket.app.ui.theme.listingTypeColor
import com.matrixmarket.app.util.DateFormats
import com.matrixmarket.app.util.InputValidator
import com.matrixmarket.app.util.Resource
import com.matrixmarket.app.util.ViewModelFactory

@Composable
fun ListingDetailScreen(
    listingId: Int,
    onBack: () -> Unit,
    onOfferCreated: (offerId: Int) -> Unit,
    onDeleted: () -> Unit
) {
    val viewModel: ListingDetailViewModel = viewModel(
        factory = ViewModelFactory {
            ListingDetailViewModel(
                AppContainer.listingRepository, AppContainer.tradeRepository,
                AppContainer.watchlistRepository, AppContainer.sessionManager
            )
        }
    )
    val listingState by viewModel.listingState.collectAsState()
    val offerState by viewModel.offerState.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()

    LaunchedEffect(listingId) { viewModel.load(listingId) }

    LaunchedEffect(offerState.createdOfferId) {
        offerState.createdOfferId?.let { id ->
            viewModel.onOfferNavigationHandled()
            onOfferCreated(id)
        }
    }
    LaunchedEffect(deleteState) { if (deleteState is Resource.Success) onDeleted() }

    Surface(color = LavenderLight, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                Spacer(Modifier.weight(1f))
                val current = listingState
                if (current is Resource.Success && currentUserId != null &&
                    current.data.sellerId != currentUserId && !current.fromCache
                ) {
                    IconButton(onClick = { viewModel.toggleSaved(listingId) }) {
                        Icon(
                            if (isSaved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = stringResource(if (isSaved) R.string.remove_from_saved else R.string.save_listing),
                            tint = RoyalPurple
                        )
                    }
                }
            }

            when (val state = listingState) {
                is Resource.Loading, Resource.Idle -> LoadingBox()
                is Resource.Error -> ErrorBox(state.displayText(), onRetry = { viewModel.retry(listingId) })
                is Resource.Success -> ListingDetailContent(
                    listing = state.data,
                    fromCache = state.fromCache,
                    isOwnListing = state.data.sellerId == currentUserId,
                    offerState = offerState,
                    deleteState = deleteState,
                    onMakeOffer = { amount -> viewModel.makeOffer(state.data, amount) },
                    onDelete = { viewModel.delete(listingId) }
                )
            }
        }
    }
}

@Composable
private fun ListingDetailContent(
    listing: ListingResponse,
    fromCache: Boolean,
    isOwnListing: Boolean,
    offerState: OfferUiState,
    deleteState: Resource<Unit>,
    onMakeOffer: (String) -> Unit,
    onDelete: () -> Unit
) {
    val typeColor = listingTypeColor(listing.listingType)
    var offerAmount by rememberSaveable(listing.listingId) {
        mutableStateOf(if (listing.price > 0) String.format(java.util.Locale.US, "%.2f", listing.price) else "0")
    }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp)
    ) {
        ListingImage(
            imageUrl = listing.imageUrl,
            placeholderText = categoryLabel(listing.categoryId, listing.categoryName),
            accent = typeColor,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(220.dp)
        )

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(listingTypeLabel(listing.listingType), typeColor)
            Spacer(Modifier.width(8.dp))
            Pill(categoryLabel(listing.categoryId, listing.categoryName), RoyalPurple, filled = false)
        }
        Spacer(Modifier.height(8.dp))
        Text(listing.title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(DateFormats.formatRand(listing.price), color = RoyalPurple, style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(LavenderCard, CircleShape), contentAlignment = Alignment.Center) {
                Text(listing.sellerName.take(1).uppercase(), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(listing.sellerName, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = RoyalPurple, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.student_score_points, listing.sellerKarma), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.description), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(listing.description, style = MaterialTheme.typography.bodyLarge)
        listing.isbn?.let {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.isbn_value, it), style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Spacer(Modifier.height(20.dp))

        when {
            fromCache -> InfoCard(stringResource(R.string.offline_detail_note), WarningAmber)
            isOwnListing -> {
                InfoCard(stringResource(R.string.your_listing_note), RoyalPurple)
                if (listing.status == "Active") {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        enabled = deleteState !is Resource.Loading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = ErrorRed)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.delete_listing), color = ErrorRed)
                    }
                    if (deleteState is Resource.Error) {
                        Spacer(Modifier.height(6.dp))
                        Text(deleteState.displayText(), color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            listing.status != "Active" -> InfoCard(stringResource(R.string.listing_unavailable), ErrorRed)
            else -> {
                OutlinedTextField(
                    value = offerAmount,
                    onValueChange = { offerAmount = InputValidator.filterPriceInput(it) },
                    label = { Text(stringResource(R.string.your_offer)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                offerState.error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it.asString(), color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { onMakeOffer(offerAmount) },
                    enabled = !offerState.isSending,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (offerState.isSending) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.make_offer_schedule), color = Color.White)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_listing)) },
            text = { Text(stringResource(R.string.delete_listing_confirm)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text(stringResource(R.string.delete), color = ErrorRed)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun InfoCard(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
    }
}
