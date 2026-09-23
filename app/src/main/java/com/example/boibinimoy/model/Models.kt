package com.example.boibinimoy.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
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
    val coverUrl: String = "",        // Firestore / Storage URL
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
    val isbn: String = "",
    val isSold: Boolean = false,
    val sellerId: String = "user_default"
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

data class BookRequest(
    val id: String = "",
    val bookTitle: String = "",
    val author: String = "",
    val category: String = "General",
    val maxPrice: Int = 0,
    val requesterName: String = "",
    val requesterLocation: String = "",
    val note: String = "",
    val timestamp: String = "",
    val status: String = "OPEN"       // OPEN, FULFILLED, CLOSED
) : Serializable

data class OrderItem(
    val id: String = "",
    val items: List<CartItem> = emptyList(),
    val totalAmount: Int = 0,
    val orderDate: String = "",
    val status: String = "CONFIRMED",
    val userName: String = "",
    val deliveryAddress: String = ""
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
    val type: String = "SYSTEM",     // EXCHANGE, PRICE_DROP, MESSAGE, ORDER, SYSTEM, BOOK_REQUEST
    var isRead: Boolean = false,
    val userId: String = "default"
) : Serializable

enum class NotificationType {
    EXCHANGE, PRICE_DROP, MESSAGE, ORDER, SYSTEM, BOOK_REQUEST
}

@IgnoreExtraProperties
data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    @get:PropertyName("isVerified") @set:PropertyName("isVerified") var isVerified: Boolean = false,
    val memberSince: String = "",
    val rating: Double = 0.0,
    val reviewsCount: Int = 0,
    val booksListed: Int = 0,
    val booksSold: Int = 0,
    val booksExchanged: Int = 0,
    val totalSavingsTaka: Int = 0,
    @get:PropertyName("isAdmin") @set:PropertyName("isAdmin") var isAdmin: Boolean = false,
    val passwordHash: String = ""
) : Serializable
