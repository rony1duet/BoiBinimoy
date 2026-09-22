package com.example.boibinimoy.model

import java.io.Serializable

// ---------------------------------------------------------------------------
// Firestore-compatible models
// All fields have default values so Firestore can deserialize them with
// no-arg constructors. coverResId is kept for local fallback rendering.
// ---------------------------------------------------------------------------

data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val price: Int = 0,
    val originalPrice: Int = 0,
    val coverResId: Int = 0,          // local drawable fallback
    val coverUrl: String = "",        // Firestore / Storage URL (future)
    val category: String = "",
    val condition: String = "Good",
    val location: String = "",
    val sellerName: String = "",
    val sellerRating: Double = 5.0,
    val sellerReviewCount: Int = 0,
    val sellerResponseTime: String = "Within a day",
    val isSellerVerified: Boolean = false,
    val isExchangeAvailable: Boolean = false,
    val exchangeLookingFor: String = "",
    var isFavorite: Boolean = false,
    val description: String = "",
    val publisher: String = "",
    val language: String = "English",
    val publishedYear: String = "2020",
    val pageCount: Int = 200,
    val isbn: String = ""
) : Serializable

data class Category(
    val id: String = "",
    val name: String = "",
    val iconResId: Int = 0,
    val backgroundColor: Int = 0,
    val bookCount: Int = 0
) : Serializable

data class ExchangeRequest(
    val id: String = "",
    val requestedBookId: String = "",
    val requestedBookTitle: String = "",
    val offeredBookId: String = "",
    val offeredBookTitle: String = "",
    val userName: String = "",
    val userLocation: String = "",
    val status: String = "PENDING",   // PENDING, ACCEPTED, REJECTED
    val date: String = "",
    val message: String = ""
) : Serializable

data class CartItem(
    val book: Book = Book(),
    var quantity: Int = 1
) : Serializable

data class ChatMessage(
    val id: String = "",
    val chatId: String = "",
    val senderName: String = "",
    val senderId: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val isMe: Boolean = false
) : Serializable

data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: String = "",
    val type: String = "SYSTEM",     // EXCHANGE, PRICE_DROP, MESSAGE, ORDER, SYSTEM
    var isRead: Boolean = false
) : Serializable

enum class NotificationType {
    EXCHANGE, PRICE_DROP, MESSAGE, ORDER, SYSTEM
}

data class UserProfile(
    val id: String = "",
    val name: String = "Riad Hasan",
    val email: String = "riad.hasan@example.com",
    val phone: String = "+880 1712-345678",
    val location: String = "Dhanmondi, Dhaka",
    val isVerified: Boolean = true,
    val memberSince: String = "January 2024",
    val rating: Double = 4.9,
    val reviewsCount: Int = 38,
    val booksListed: Int = 12,
    val booksSold: Int = 28,
    val booksExchanged: Int = 15,
    val totalSavingsTaka: Int = 3450
) : Serializable
