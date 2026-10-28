package com.matrixmarket.app.data.remote.dto

data class CategoryDto(
    val categoryId: Int,
    val categoryName: String,
    val description: String?
)

data class CreateListingRequest(
    val categoryId: Int,
    val title: String,
    val description: String,
    val price: Double,
    val listingType: String = "Sell",
    val isbn: String? = null,
    val imageUrl: String? = null
)

data class ListingResponse(
    val listingId: Int,
    val title: String,
    val description: String,
    val price: Double,
    val listingType: String,
    val isbn: String?,
    val imageUrl: String?,
    val status: String,
    val categoryId: Int,
    val categoryName: String,
    val sellerName: String,
    val sellerId: Int,
    val sellerKarma: Int,
    val createdAt: String
)

data class BookInfoResponse(
    val isbn: String,
    val title: String,
    val authors: List<String>?,
    val publisher: String?,
    val publishedDate: String?,
    val description: String?
)
