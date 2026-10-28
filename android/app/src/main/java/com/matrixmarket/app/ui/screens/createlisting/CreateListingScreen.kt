package com.matrixmarket.app.ui.screens.createlisting

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.matrixmarket.app.R
import com.matrixmarket.app.di.AppContainer
import com.matrixmarket.app.ui.components.ScreenHeader
import com.matrixmarket.app.ui.components.SectionTitle
import com.matrixmarket.app.ui.components.asString
import com.matrixmarket.app.ui.components.categoryLabel
import com.matrixmarket.app.ui.components.listingTypeLabel
import com.matrixmarket.app.ui.theme.ErrorRed
import com.matrixmarket.app.ui.theme.LavenderLight
import com.matrixmarket.app.ui.theme.RoyalPurple
import com.matrixmarket.app.ui.theme.SuccessGreen
import com.matrixmarket.app.ui.theme.TextMuted
import com.matrixmarket.app.ui.theme.listingTypeColor
import com.matrixmarket.app.util.ImageUtils
import com.matrixmarket.app.util.ViewModelFactory
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateListingScreen(
    scannedIsbn: String?,
    onScannedIsbnConsumed: () -> Unit,
    onScanIsbnClick: () -> Unit,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: CreateListingViewModel = viewModel(
        factory = ViewModelFactory { CreateListingViewModel(AppContainer.listingRepository, AppContainer.appContext) }
    )
    val state by viewModel.state.collectAsState()
    val categories by viewModel.categories.collectAsState()

    // A barcode came back from the scanner screen.
    LaunchedEffect(scannedIsbn) {
        if (!scannedIsbn.isNullOrBlank()) {
            viewModel.onIsbnScanned(scannedIsbn)
            onScannedIsbnConsumed()
        }
    }

    val publishedMsg = stringResource(R.string.listing_published)
    val photoFailedMsg = stringResource(R.string.listing_published_photo_failed)
    val offlineMsg = stringResource(R.string.listing_saved_offline)
    LaunchedEffect(state.outcome) {
        val outcome = state.outcome ?: return@LaunchedEffect
        val message = when (outcome) {
            CreateOutcome.PUBLISHED -> publishedMsg
            CreateOutcome.PUBLISHED_PHOTO_FAILED -> photoFailedMsg
            CreateOutcome.SAVED_OFFLINE -> offlineMsg
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        viewModel.consumeOutcome()
        onFinished()
    }

    // ---- Photo pickers (gallery needs no permission; camera does) ----
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onPhotoPicked(uri)
    }
    var pendingCameraPath by rememberSaveable { mutableStateOf<String?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val path = pendingCameraPath
        if (saved && path != null) viewModel.onPhotoPicked(Uri.fromFile(File(path)))
        pendingCameraPath = null
    }
    fun launchCamera() {
        val file = ImageUtils.newCameraFile(context)
        pendingCameraPath = file.absolutePath
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        try {
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, R.string.error_no_camera, Toast.LENGTH_SHORT).show()
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
        else Toast.makeText(context, R.string.camera_permission_needed, Toast.LENGTH_SHORT).show()
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
    val typeColor = listingTypeColor(state.listingType)

    Surface(color = LavenderLight, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp)
        ) {
            ScreenHeader(stringResource(R.string.selling), stringResource(R.string.create_new_listing))
            Spacer(Modifier.height(16.dp))

            // ---- Photo (User Defined Feature 4) ----
            SectionTitle(stringResource(R.string.photo))
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                val photo = state.photoPath
                when {
                    state.isPreparingPhoto -> CircularProgressIndicator(color = RoyalPurple)
                    photo != null -> {
                        AsyncImage(
                            model = File(photo),
                            contentDescription = stringResource(R.string.photo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { viewModel.removePhoto() },
                            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                        ) {
                            Surface(color = Color.Black.copy(alpha = 0.55f), shape = CircleShape) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove_photo), tint = Color.White, modifier = Modifier.padding(4.dp))
                            }
                        }
                    }
                    else -> Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(R.string.add_photo_hint), color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.gallery))
                }
                OutlinedButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            launchCamera()
                        } else {
                            cameraPermission.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.camera))
                }
            }

            // ---- ISBN (User Defined Feature 1) ----
            Spacer(Modifier.height(18.dp))
            SectionTitle(stringResource(R.string.textbook_isbn))
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onScanIsbnClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.scan_isbn))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.isbn,
                onValueChange = viewModel::onIsbnChanged,
                label = { Text(stringResource(R.string.isbn_optional)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    if (state.isLookingUpBook) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { viewModel.lookupBook() }, enabled = state.isbn.isNotBlank()) {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.look_up_book))
                        }
                    }
                },
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )
            state.bookMessage?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    if (state.bookFound) stringResource(R.string.book_found, it.asString()) else it.asString(),
                    color = if (state.bookFound) SuccessGreen else TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // ---- Details ----
            Spacer(Modifier.height(18.dp))
            SectionTitle(stringResource(R.string.details))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.title, onValueChange = viewModel::onTitleChanged,
                label = { Text(stringResource(R.string.title)) }, singleLine = true,
                colors = fieldColors, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.description, onValueChange = viewModel::onDescriptionChanged,
                label = { Text(stringResource(R.string.description)) }, minLines = 3,
                colors = fieldColors, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.price, onValueChange = viewModel::onPriceChanged,
                label = { Text(stringResource(R.string.price_rand)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = fieldColors, modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))
            SectionTitle(stringResource(R.string.category))
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { category ->
                    FilterChip(
                        selected = state.categoryId == category.categoryId,
                        onClick = { viewModel.onCategorySelected(category.categoryId) },
                        label = { Text(categoryLabel(category.categoryId, category.categoryName)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalPurple, selectedLabelColor = Color.White)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            SectionTitle(stringResource(R.string.listing_type))
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Sell", "Trade", "Lend").forEach { type ->
                    val color = listingTypeColor(type)
                    FilterChip(
                        selected = state.listingType == type,
                        onClick = { viewModel.onTypeSelected(type) },
                        label = { Text(listingTypeLabel(type)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = color, selectedLabelColor = Color.White),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = state.listingType == type,
                            borderColor = color,
                            selectedBorderColor = color
                        )
                    )
                }
            }

            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it.asString(), color = ErrorRed, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { viewModel.submit() },
                enabled = !state.isSubmitting && !state.isPreparingPhoto,
                colors = ButtonDefaults.buttonColors(containerColor = typeColor),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.publish_listing), color = Color.White)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
