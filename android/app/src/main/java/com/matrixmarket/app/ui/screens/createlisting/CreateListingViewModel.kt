package com.matrixmarket.app.ui.screens.createlisting

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.matrixmarket.app.R
import com.matrixmarket.app.data.remote.dto.CategoryDto
import com.matrixmarket.app.data.remote.dto.CreateListingRequest
import com.matrixmarket.app.data.repository.CreateListingResult
import com.matrixmarket.app.data.repository.ListingRepository
import com.matrixmarket.app.ui.components.UiText
import com.matrixmarket.app.util.ImageUtils
import com.matrixmarket.app.util.InputValidator
import com.matrixmarket.app.util.IsbnValidator
import com.matrixmarket.app.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class CreateOutcome { PUBLISHED, PUBLISHED_PHOTO_FAILED, SAVED_OFFLINE }

data class CreateListingUiState(
    val title: String = "",
    val description: String = "",
    val price: String = "",
    val listingType: String = "Sell",
    val categoryId: Int? = null,
    val isbn: String = "",
    val photoPath: String? = null,
    val isPreparingPhoto: Boolean = false,
    val isLookingUpBook: Boolean = false,
    val bookMessage: UiText? = null,
    val bookFound: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: UiText? = null,
    val outcome: CreateOutcome? = null
)

// "Sell" tab. Covers User Defined Feature 1 (ISBN scanner + book lookup) and
// User Defined Feature 4 (listing photos stored in blob storage), and saves the
// listing offline when there is no connection.
class CreateListingViewModel(
    private val repository: ListingRepository,
    private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(CreateListingUiState())
    val state: StateFlow<CreateListingUiState> = _state.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryDto>>(emptyList())
    val categories: StateFlow<List<CategoryDto>> = _categories.asStateFlow()

    init {
        viewModelScope.launch { _categories.value = repository.getCategories() }
    }

    fun onTitleChanged(v: String) = _state.update { it.copy(title = v.take(120), error = null) }
    fun onDescriptionChanged(v: String) = _state.update { it.copy(description = v.take(2000), error = null) }
    fun onPriceChanged(v: String) = _state.update { it.copy(price = InputValidator.filterPriceInput(v), error = null) }
    fun onTypeSelected(v: String) = _state.update { it.copy(listingType = v, error = null) }
    fun onCategorySelected(id: Int) = _state.update { it.copy(categoryId = id, error = null) }

    fun onIsbnChanged(v: String) = _state.update {
        it.copy(isbn = v.filter { c -> c.isDigit() || c == 'X' || c == 'x' || c == '-' }.take(17), bookMessage = null, bookFound = false, error = null)
    }

    // The scanner only hands us barcodes that already passed IsbnValidator.
    fun onIsbnScanned(isbn: String) {
        _state.update { it.copy(isbn = isbn, categoryId = it.categoryId ?: TEXTBOOKS_CATEGORY_ID) }
        lookupBook()
    }

    // Asks the API (Google Books) for the title so the listing fills itself in.
    fun lookupBook() {
        val isbn = IsbnValidator.normalize(_state.value.isbn)
        if (!IsbnValidator.isValid(isbn)) {
            _state.update { it.copy(bookMessage = UiText.Res(R.string.error_invalid_isbn), bookFound = false) }
            return
        }
        _state.update { it.copy(isLookingUpBook = true, bookMessage = null) }
        viewModelScope.launch {
            when (val result = repository.lookupBook(isbn)) {
                is Resource.Success -> {
                    val book = result.data
                    val authors = book.authors.orEmpty().joinToString(", ")
                    _state.update { s ->
                        s.copy(
                            isLookingUpBook = false,
                            bookFound = true,
                            bookMessage = UiText.Plain(if (authors.isBlank()) book.title else "${book.title} - $authors"),
                            title = if (s.title.isBlank()) book.title.take(120) else s.title,
                            description = if (s.description.isBlank() && authors.isNotBlank()) {
                                appContext.getString(R.string.book_description_template, authors)
                            } else s.description,
                            categoryId = s.categoryId ?: TEXTBOOKS_CATEGORY_ID
                        )
                    }
                }
                is Resource.Error -> _state.update {
                    it.copy(
                        isLookingUpBook = false,
                        bookFound = false,
                        bookMessage = if (result.isNetworkError) UiText.Res(R.string.book_lookup_offline) else UiText.Plain(result.message)
                    )
                }
                else -> _state.update { it.copy(isLookingUpBook = false) }
            }
        }
    }

    fun onPhotoPicked(uri: Uri) {
        _state.update { it.copy(isPreparingPhoto = true) }
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) { ImageUtils.prepareListingPhoto(appContext, uri) }
            val previous = _state.value.photoPath
            _state.update {
                if (file != null) it.copy(isPreparingPhoto = false, photoPath = file.absolutePath)
                else it.copy(isPreparingPhoto = false, error = UiText.Res(R.string.error_photo))
            }
            if (file != null && previous != null) File(previous).delete()
        }
    }

    fun removePhoto() {
        _state.value.photoPath?.let { File(it).delete() }
        _state.update { it.copy(photoPath = null) }
    }

    fun submit() {
        val s = _state.value
        val price = InputValidator.parsePrice(s.price)
        val isbn = IsbnValidator.normalize(s.isbn)
        val errorRes = when {
            !InputValidator.isValidTitle(s.title) -> R.string.error_title
            !InputValidator.isValidDescription(s.description) -> R.string.error_description
            price == null -> R.string.error_price
            s.listingType == "Sell" && price <= 0.0 -> R.string.error_price_zero
            s.categoryId == null -> R.string.error_category
            isbn.isNotEmpty() && !IsbnValidator.isValid(isbn) -> R.string.error_invalid_isbn
            else -> null
        }
        if (errorRes != null || price == null || s.categoryId == null) {
            _state.update { it.copy(error = UiText.Res(errorRes ?: R.string.error_price)) }
            return
        }

        val request = CreateListingRequest(
            categoryId = s.categoryId,
            title = s.title.trim(),
            description = s.description.trim(),
            price = price,
            listingType = s.listingType,
            isbn = isbn.ifEmpty { null }
        )
        val categoryName = _categories.value.firstOrNull { it.categoryId == s.categoryId }?.categoryName.orEmpty()
        val photo = s.photoPath?.let { File(it) }

        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.createListing(request, categoryName, photo)) {
                is CreateListingResult.Published -> {
                    Log.i(TAG, "Published listing ${result.listing.listingId}")
                    _state.value = CreateListingUiState(
                        outcome = if (result.photoUploadFailed) CreateOutcome.PUBLISHED_PHOTO_FAILED else CreateOutcome.PUBLISHED
                    )
                }
                CreateListingResult.SavedOffline -> {
                    _state.value = CreateListingUiState(outcome = CreateOutcome.SAVED_OFFLINE)
                }
                is CreateListingResult.Failed -> {
                    _state.update { it.copy(isSubmitting = false, error = UiText.Plain(result.message)) }
                }
            }
        }
    }

    fun consumeOutcome() = _state.update { it.copy(outcome = null) }

    private companion object {
        const val TAG = "CreateListingVM"
        const val TEXTBOOKS_CATEGORY_ID = 1
    }
}
