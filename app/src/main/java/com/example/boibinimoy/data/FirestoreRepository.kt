package com.example.boibinimoy.data

import com.example.boibinimoy.R
import com.example.boibinimoy.model.Book
import com.example.boibinimoy.model.Category
import com.example.boibinimoy.model.ChatMessage
import com.example.boibinimoy.model.NotificationItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()

    // -----------------------------------------------------------------------
    // Collections
    // -----------------------------------------------------------------------
    private val booksRef = db.collection("books")
    private val categoriesRef = db.collection("categories")
    private val chatsRef = db.collection("chats")
    private val notificationsRef = db.collection("notifications")
    private val usersRef = db.collection("users")

    // -----------------------------------------------------------------------
    // Books
    // -----------------------------------------------------------------------

    /** Real-time Flow of all books */
    fun getBooks(): Flow<List<Book>> = callbackFlow {
        val listener = booksRef
            .orderBy("title")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error); return@addSnapshotListener
                }
                val books = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Book::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(books)
            }
        awaitClose { listener.remove() }
    }

    /** One-shot fetch — useful for search */
    suspend fun fetchBooks(): List<Book> {
        return try {
            booksRef.orderBy("title").get().await().documents.mapNotNull { doc ->
                doc.toObject(Book::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            BookRepository.getAllBooks()  // fallback to local
        }
    }

    /** Add / update a book document */
    suspend fun addBook(book: Book): String {
        val docRef = if (book.id.isBlank()) booksRef.document() else booksRef.document(book.id)
        val newBook = book.copy(id = docRef.id)
        docRef.set(newBook).await()
        return docRef.id
    }

    // -----------------------------------------------------------------------
    // Categories
    // -----------------------------------------------------------------------

    fun getCategories(): Flow<List<Category>> = callbackFlow {
        val listener = categoriesRef
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val cats = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Category::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(cats)
            }
        awaitClose { listener.remove() }
    }

    suspend fun fetchCategories(): List<Category> {
        return try {
            categoriesRef.orderBy("name").get().await().documents.mapNotNull { doc ->
                doc.toObject(Category::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            BookRepository.getCategories()
        }
    }

    // -----------------------------------------------------------------------
    // Chat
    // -----------------------------------------------------------------------

    /** Real-time chat messages for a given chatId */
    fun getChatMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = chatsRef
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val msgs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(msgs)
            }
        awaitClose { listener.remove() }
    }

    /** Send a new chat message */
    suspend fun sendMessage(chatId: String, message: ChatMessage) {
        val msgRef = chatsRef.document(chatId).collection("messages").document()
        val newMsg = message.copy(id = msgRef.id, timestamp = System.currentTimeMillis())
        msgRef.set(newMsg).await()
    }

    // -----------------------------------------------------------------------
    // Notifications
    // -----------------------------------------------------------------------

    fun getNotifications(userId: String = "default"): Flow<List<NotificationItem>> = callbackFlow {
        val listener = notificationsRef
            .whereEqualTo("userId", userId)
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val notifs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(NotificationItem::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(notifs)
            }
        awaitClose { listener.remove() }
    }

    // -----------------------------------------------------------------------
    // Seeding — called once when collections are empty
    // -----------------------------------------------------------------------

    suspend fun seedIfEmpty() {
        val booksSnap = booksRef.limit(1).get().await()
        if (booksSnap.isEmpty) {
            seedBooks()
        }
        val catsSnap = categoriesRef.limit(1).get().await()
        if (catsSnap.isEmpty) {
            seedCategories()
        }
        val notifsSnap = notificationsRef.limit(1).get().await()
        if (notifsSnap.isEmpty) {
            seedNotifications()
        }
        val chatsSnap = chatsRef.document("demo_chat").collection("messages").limit(1).get().await()
        if (chatsSnap.isEmpty) {
            seedChat()
        }
    }

    private suspend fun seedBooks() {
        val books = listOf(
            Book(
                title = "The Alchemist",
                author = "Paulo Coelho",
                price = 250,
                originalPrice = 550,
                coverResId = R.drawable.cover_alchemist,
                category = "Fiction",
                condition = "Like New",
                location = "Dhanmondi, Dhaka",
                sellerName = "Tanvir Ahmed",
                sellerRating = 4.9,
                sellerReviewCount = 38,
                sellerResponseTime = "Within 1 hour",
                isSellerVerified = true,
                isExchangeAvailable = true,
                exchangeLookingFor = "Sapiens or Deep Work",
                description = "The Alchemist follows the journey of Santiago, an Andalusian shepherd boy who dreams of travelling the world. This is a timeless classic about following your dreams.",
                publisher = "HarperOne",
                language = "English",
                publishedYear = "1988",
                pageCount = 208,
                isbn = "978-0062315007"
            ),
            Book(
                title = "Sapiens",
                author = "Yuval Noah Harari",
                price = 300,
                originalPrice = 700,
                coverResId = R.drawable.cover_sapiens,
                category = "Science",
                condition = "Good",
                location = "Gulshan, Dhaka",
                sellerName = "Fatima Islam",
                sellerRating = 4.7,
                sellerReviewCount = 22,
                sellerResponseTime = "Within 2 hours",
                isSellerVerified = true,
                isExchangeAvailable = false,
                description = "A Brief History of Humankind — traces the entire history of the human species from the Stone Age through to the twenty-first century.",
                publisher = "Harper",
                language = "English",
                publishedYear = "2011",
                pageCount = 443,
                isbn = "978-0062316097"
            ),
            Book(
                title = "Rich Dad Poor Dad",
                author = "Robert T. Kiyosaki",
                price = 200,
                originalPrice = 450,
                coverResId = R.drawable.cover_rich_dad,
                category = "Business",
                condition = "Good",
                location = "Mirpur, Dhaka",
                sellerName = "Mehedi Hasan",
                sellerRating = 4.5,
                sellerReviewCount = 15,
                sellerResponseTime = "Within a day",
                isSellerVerified = false,
                isExchangeAvailable = true,
                exchangeLookingFor = "Atomic Habits or The Psychology of Money",
                description = "Rich Dad Poor Dad advocates financial independence and building wealth through investing, real estate, owning businesses, and using finance protection tactics.",
                publisher = "Warner Books Ed",
                language = "English",
                publishedYear = "1997",
                pageCount = 207,
                isbn = "978-1612680194"
            ),
            Book(
                title = "Atomic Habits",
                author = "James Clear",
                price = 280,
                originalPrice = 600,
                coverResId = R.drawable.cover_atomic_habits,
                category = "Business",
                condition = "Like New",
                location = "Banani, Dhaka",
                sellerName = "Sumaiya Akter",
                sellerRating = 5.0,
                sellerReviewCount = 47,
                sellerResponseTime = "Within 30 min",
                isSellerVerified = true,
                isExchangeAvailable = false,
                description = "Tiny Changes, Remarkable Results. No matter your goals, Atomic Habits offers a proven framework for improving — every day.",
                publisher = "Avery",
                language = "English",
                publishedYear = "2018",
                pageCount = 319,
                isbn = "978-0735211292"
            ),
            Book(
                title = "Padma Nadir Majhi",
                author = "Manik Bandyopadhyay",
                price = 150,
                originalPrice = 320,
                coverResId = R.drawable.cover_padma_nadir,
                category = "Fiction",
                condition = "Acceptable",
                location = "Old Dhaka",
                sellerName = "Karim Uddin",
                sellerRating = 4.3,
                sellerReviewCount = 8,
                sellerResponseTime = "Within a day",
                isSellerVerified = false,
                isExchangeAvailable = true,
                exchangeLookingFor = "Any Bengali Classic",
                description = "পদ্মা নদীর মাঝি — মাণিক বন্দ্যোপাধ্যায়ের লেখা একটি বিখ্যাত উপন্যাস যা পদ্মা নদীর জেলেদের জীবনকে কেন্দ্র করে।",
                publisher = "Ananda Publishers",
                language = "Bengali",
                publishedYear = "1936",
                pageCount = 256,
                isbn = "978-8177567021"
            ),
            Book(
                title = "Debi",
                author = "Humayun Ahmed",
                price = 120,
                originalPrice = 250,
                coverResId = R.drawable.cover_debi,
                category = "Fiction",
                condition = "Good",
                location = "Uttara, Dhaka",
                sellerName = "Nadia Rahman",
                sellerRating = 4.6,
                sellerReviewCount = 19,
                sellerResponseTime = "Within 3 hours",
                isSellerVerified = true,
                isExchangeAvailable = false,
                description = "দেবী — হুমায়ূন আহমেদের মিসির আলি সিরিজের একটি জনপ্রিয় উপন্যাস।",
                publisher = "Anyaprakash",
                language = "Bengali",
                publishedYear = "1985",
                pageCount = 180,
                isbn = "978-9840410070"
            ),
            Book(
                title = "Think and Grow Rich",
                author = "Napoleon Hill",
                price = 180,
                originalPrice = 400,
                coverResId = R.drawable.cover_rich_dad,  // reuse cover
                category = "Business",
                condition = "Good",
                location = "Mohakhali, Dhaka",
                sellerName = "Arif Hossain",
                sellerRating = 4.4,
                sellerReviewCount = 12,
                sellerResponseTime = "Within 2 hours",
                isSellerVerified = false,
                isExchangeAvailable = true,
                exchangeLookingFor = "Any self-help book",
                description = "Napoleon Hill's timeless classic on achieving success through the power of thought, desire, faith, and persistence.",
                publisher = "Sound Wisdom",
                language = "English",
                publishedYear = "1937",
                pageCount = 238,
                isbn = "978-0143110583"
            ),
            Book(
                title = "1984",
                author = "George Orwell",
                price = 220,
                originalPrice = 480,
                coverResId = R.drawable.cover_alchemist,  // reuse cover
                category = "Fiction",
                condition = "Like New",
                location = "Khilgaon, Dhaka",
                sellerName = "Tasneem Haque",
                sellerRating = 4.8,
                sellerReviewCount = 31,
                sellerResponseTime = "Within 1 hour",
                isSellerVerified = true,
                isExchangeAvailable = true,
                exchangeLookingFor = "Brave New World or Animal Farm",
                description = "A dystopian social science fiction novel following the life of Winston Smith in a totalitarian society ruled by Big Brother.",
                publisher = "Secker & Warburg",
                language = "English",
                publishedYear = "1949",
                pageCount = 328,
                isbn = "978-0451524935"
            )
        )

        for (book in books) {
            val docRef = booksRef.document()
            docRef.set(book.copy(id = docRef.id)).await()
        }
    }

    private suspend fun seedCategories() {
        val categories = listOf(
            mapOf("name" to "Academic", "iconResId" to R.drawable.ic_cat_academic, "bookCount" to 234),
            mapOf("name" to "Fiction", "iconResId" to R.drawable.ic_cat_fiction, "bookCount" to 187),
            mapOf("name" to "Historical", "iconResId" to R.drawable.ic_cat_history, "bookCount" to 142),
            mapOf("name" to "Poetry", "iconResId" to R.drawable.ic_cat_poetry, "bookCount" to 98),
            mapOf("name" to "Science", "iconResId" to R.drawable.ic_cat_science, "bookCount" to 156),
            mapOf("name" to "Comics", "iconResId" to R.drawable.ic_cat_comics, "bookCount" to 73),
            mapOf("name" to "Business", "iconResId" to R.drawable.ic_cat_business, "bookCount" to 121),
            mapOf("name" to "Islamic", "iconResId" to R.drawable.ic_cat_islamic, "bookCount" to 89)
        )
        for (cat in categories) {
            val docRef = categoriesRef.document()
            categoriesRef.document(docRef.id).set(cat + mapOf("id" to docRef.id)).await()
        }
    }

    private suspend fun seedNotifications() {
        val notifs = listOf(
            mapOf(
                "title" to "Exchange Request!",
                "message" to "Mehedi wants to exchange 'Rich Dad Poor Dad' for your 'Atomic Habits'",
                "timestamp" to "2 min ago",
                "type" to "EXCHANGE",
                "isRead" to false,
                "userId" to "default"
            ),
            mapOf(
                "title" to "Price Drop Alert 🔥",
                "message" to "'Sapiens' dropped from ৳700 to ৳300 — grab it now!",
                "timestamp" to "15 min ago",
                "type" to "PRICE_DROP",
                "isRead" to false,
                "userId" to "default"
            ),
            mapOf(
                "title" to "New Message",
                "message" to "Tanvir: 'Is The Alchemist still available?'",
                "timestamp" to "1 hour ago",
                "type" to "MESSAGE",
                "isRead" to true,
                "userId" to "default"
            ),
            mapOf(
                "title" to "Order Confirmed ✅",
                "message" to "Your order for 'Debi' has been confirmed. Seller will contact you soon.",
                "timestamp" to "Yesterday",
                "type" to "ORDER",
                "isRead" to true,
                "userId" to "default"
            )
        )
        for (notif in notifs) {
            val docRef = notificationsRef.document()
            notificationsRef.document(docRef.id).set(notif + mapOf("id" to docRef.id)).await()
        }
    }

    private suspend fun seedChat() {
        val messages = listOf(
            mapOf(
                "senderName" to "Tanvir Ahmed",
                "senderId" to "seller_001",
                "message" to "Hi! Is The Alchemist still available?",
                "timestamp" to (System.currentTimeMillis() - 3600000),
                "isMe" to false,
                "chatId" to "demo_chat"
            ),
            mapOf(
                "senderName" to "You",
                "senderId" to "me",
                "message" to "Yes, it's available! Condition is Like New.",
                "timestamp" to (System.currentTimeMillis() - 3500000),
                "isMe" to true,
                "chatId" to "demo_chat"
            ),
            mapOf(
                "senderName" to "Tanvir Ahmed",
                "senderId" to "seller_001",
                "message" to "Great! Can we meet at Dhanmondi tomorrow?",
                "timestamp" to (System.currentTimeMillis() - 3400000),
                "isMe" to false,
                "chatId" to "demo_chat"
            ),
            mapOf(
                "senderName" to "You",
                "senderId" to "me",
                "message" to "Sure, 5 PM works for me! I'll be at Dhanmondi Lake.",
                "timestamp" to (System.currentTimeMillis() - 3300000),
                "isMe" to true,
                "chatId" to "demo_chat"
            )
        )
        val chatMsgsRef = chatsRef.document("demo_chat").collection("messages")
        for (msg in messages) {
            val docRef = chatMsgsRef.document()
            chatMsgsRef.document(docRef.id).set(msg + mapOf("id" to docRef.id)).await()
        }
    }
}
