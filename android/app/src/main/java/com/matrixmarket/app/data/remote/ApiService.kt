package com.matrixmarket.app.data.remote

import com.matrixmarket.app.data.remote.dto.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

// The full contract against the Matrix Market REST API (ASP.NET Core 8, hosted on Azure).
interface ApiService {

    // ---- Auth ----
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/v1/auth/sso-login")
    suspend fun ssoLogin(@Body request: SsoLoginRequest): Response<AuthResponse>

    // ---- Users / settings / push token ----
    @GET("api/v1/users/{userId}/profile")
    suspend fun getProfile(@Path("userId") userId: Int): Response<UserProfileResponse>

    @PUT("api/v1/users/{userId}/settings")
    suspend fun updateSettings(
        @Path("userId") userId: Int,
        @Body request: UpdateSettingsRequest
    ): Response<UpdateSettingsResponse>

    @PUT("api/v1/users/me/device-token")
    suspend fun saveDeviceToken(@Body request: DeviceTokenRequest): Response<Unit>

    @DELETE("api/v1/users/me/device-token")
    suspend fun removeDeviceToken(): Response<Unit>

    // ---- Listings ----
    @GET("api/v1/categories")
    suspend fun getCategories(): Response<List<CategoryDto>>

    @GET("api/v1/listings")
    suspend fun getListings(
        @Query("category") categoryId: Int? = null,
        @Query("search") search: String? = null,
        @Query("type") type: String? = null
    ): Response<List<ListingResponse>>

    @GET("api/v1/listings/{id}")
    suspend fun getListingById(@Path("id") id: Int): Response<ListingResponse>

    @POST("api/v1/listings")
    suspend fun createListing(@Body request: CreateListingRequest): Response<ListingResponse>

    @Multipart
    @POST("api/v1/listings/{id}/image")
    suspend fun uploadListingImage(
        @Path("id") id: Int,
        @Part file: MultipartBody.Part
    ): Response<ListingResponse>

    @DELETE("api/v1/listings/{id}")
    suspend fun deleteListing(@Path("id") id: Int): Response<Unit>

    @GET("api/v1/books/{isbn}")
    suspend fun lookupBook(@Path("isbn") isbn: String): Response<BookInfoResponse>

    // ---- Offers & trades ----
    @POST("api/v1/offers")
    suspend fun createOffer(@Body request: CreateOfferRequest): Response<OfferResponse>

    @POST("api/v1/trades/schedule")
    suspend fun scheduleTrade(@Body request: ScheduleTradeRequest): Response<TradeMeetingResponse>

    @GET("api/v1/trades/mine")
    suspend fun getMyTradeMeetings(): Response<List<MyTradeMeetingResponse>>

    @GET("api/v1/trades/{meetingId}")
    suspend fun getTradeMeeting(@Path("meetingId") id: Int): Response<TradeMeetingResponse>

    @POST("api/v1/trades/{meetingId}/confirm")
    suspend fun confirmTrade(
        @Path("meetingId") id: Int,
        @Body request: ConfirmTradeRequest
    ): Response<ConfirmTradeResponse>

    // ---- Notifications inbox (NoSQL) ----
    @GET("api/v1/notifications")
    suspend fun getNotifications(): Response<List<NotificationResponse>>

    @GET("api/v1/notifications/unread-count")
    suspend fun getUnreadCount(): Response<UnreadCountResponse>

    @POST("api/v1/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): Response<Unit>

    @POST("api/v1/notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<Unit>

    // ---- Watchlist (NoSQL) ----
    @GET("api/v1/watchlist")
    suspend fun getWatchlist(): Response<List<ListingResponse>>

    @GET("api/v1/watchlist/ids")
    suspend fun getWatchlistIds(): Response<List<Int>>

    @POST("api/v1/watchlist/{listingId}")
    suspend fun addToWatchlist(@Path("listingId") listingId: Int): Response<Unit>

    @DELETE("api/v1/watchlist/{listingId}")
    suspend fun removeFromWatchlist(@Path("listingId") listingId: Int): Response<Unit>
}
