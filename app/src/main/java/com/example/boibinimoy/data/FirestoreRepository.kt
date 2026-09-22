package com.example.boibinimoy.data

import com.example.boibinimoy.R
import com.example.boibinimoy.model.*
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
    private val exchangeRequestsRef = db.collection("exchange_requests")
    private val bookRequestsRef = db.collection("book_requests")
    private val ordersRef = db.collection("orders")

    // -----------------------------------------------------------------------
    // Books (CRUD & Admin)
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

    /** Admin / Seller: Delete a book from Firestore */
    suspend fun deleteBook(bookId: String) {
        if (bookId.isNotBlank()) {
            booksRef.document(bookId).delete().await()
        }
    }

    /** Admin / Seller: Mark a book as sold */
    suspend fun markBookAsSold(bookId: String) {
        if (bookId.isNotBlank()) {
            booksRef.document(bookId).update("isSold", true).await()
        }
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
    // Exchange Requests
    // -----------------------------------------------------------------------

    fun getExchangeRequests(): Flow<List<ExchangeRequest>> = callbackFlow {
        val listener = exchangeRequestsRef
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val reqs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ExchangeRequest::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(reqs)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addExchangeRequest(req: ExchangeRequest): String {
        val docRef = if (req.id.isBlank()) exchangeRequestsRef.document() else exchangeRequestsRef.document(req.id)
        val newReq = req.copy(id = docRef.id)
        docRef.set(newReq).await()
        return docRef.id
    }

    suspend fun updateExchangeStatus(requestId: String, status: String) {
        if (requestId.isNotBlank()) {
            exchangeRequestsRef.document(requestId).update("status", status).await()
        }
    }

    suspend fun deleteExchangeRequest(requestId: String) {
        if (requestId.isNotBlank()) {
            exchangeRequestsRef.document(requestId).delete().await()
        }
    }

    // -----------------------------------------------------------------------
    // Book Requests (User "Request a Book" Feature)
    // -----------------------------------------------------------------------

    fun getBookRequests(): Flow<List<BookRequest>> = callbackFlow {
        val listener = bookRequestsRef
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val reqs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(BookRequest::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(reqs)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addBookRequest(request: BookRequest): String {
        val docRef = if (request.id.isBlank()) bookRequestsRef.document() else bookRequestsRef.document(request.id)
        val newReq = request.copy(id = docRef.id)
        docRef.set(newReq).await()

        // Also broadcast a notification for new book request
        sendNotification(
            NotificationItem(
                title = "New Book Request Posted 📖",
                message = "${request.requesterName} requested '${request.bookTitle}' by ${request.author}",
                timestamp = "Just now",
                type = NotificationType.BOOK_REQUEST.name
            )
        )

        return docRef.id
    }

    suspend fun deleteBookRequest(requestId: String) {
        if (requestId.isNotBlank()) {
            bookRequestsRef.document(requestId).delete().await()
        }
    }

    // -----------------------------------------------------------------------
    // Orders
    // -----------------------------------------------------------------------

    suspend fun addOrder(order: OrderItem): String {
        val docRef = ordersRef.document()
        val newOrder = order.copy(id = docRef.id)
        docRef.set(newOrder).await()

        // Trigger Order Confirmation Notification
        sendNotification(
            NotificationItem(
                title = "Order Placed Successfully! ✅",
                message = "Your order #${docRef.id.take(8).uppercase()} for ${order.items.size} book(s) total ৳${order.totalAmount} has been placed.",
                timestamp = "Just now",
                type = NotificationType.ORDER.name
            )
        )

        return docRef.id
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

    suspend fun sendNotification(notif: NotificationItem): String {
        val docRef = notificationsRef.document()
        val newNotif = notif.copy(id = docRef.id)
        docRef.set(newNotif).await()
        return docRef.id
    }

    suspend fun sendBroadcastNotification(title: String, message: String): String {
        return sendNotification(
            NotificationItem(
                title = title,
                message = message,
                timestamp = "Just now",
                type = NotificationType.SYSTEM.name,
                isRead = false,
                userId = "default"
            )
        )
    }

    // -----------------------------------------------------------------------
    // User Profile & Admin Role
    // -----------------------------------------------------------------------

    fun getUserProfile(userId: String = "user_default"): Flow<UserProfile> = callbackFlow {
        val listener = usersRef.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val profile = snapshot?.toObject(UserProfile::class.java) ?: UserProfile(id = userId)
                trySend(profile)
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateUserProfile(profile: UserProfile) {
        usersRef.document(profile.id).set(profile).await()
    }

    suspend fun toggleAdminRole(userId: String = "user_default", isAdmin: Boolean) {
        usersRef.document(userId).update("isAdmin", isAdmin).await()
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
        val exSnap = exchangeRequestsRef.limit(1).get().await()
        if (exSnap.isEmpty) {
            seedExchangeRequests()
        }
        val bkReqSnap = bookRequestsRef.limit(1).get().await()
        if (bkReqSnap.isEmpty) {
            seedBookRequests()
        }
        val userSnap = usersRef.document("user_default").get().await()
        if (!userSnap.exists()) {
            usersRef.document("user_default").set(UserProfile()).await()
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
            )
        )
        val chatMsgsRef = chatsRef.document("demo_chat").collection("messages")
        for (msg in messages) {
            val docRef = chatMsgsRef.document()
            chatMsgsRef.document(docRef.id).set(msg + mapOf("id" to docRef.id)).await()
        }
    }

    private suspend fun seedExchangeRequests() {
        val reqs = listOf(
            ExchangeRequest(
                requestedBookTitle = "The Alchemist",
                offeredBookTitle = "Sapiens",
                userName = "Kazi Shakil",
                userLocation = "Bashundhara R/A, Dhaka",
                status = "PENDING",
                date = "Today, 4:30 PM",
                message = "Hi! I have Sapiens in great condition. Would love to swap for The Alchemist."
            )
        )
        for (r in reqs) {
            val docRef = exchangeRequestsRef.document()
            docRef.set(r.copy(id = docRef.id)).await()
        }
    }

    private suspend fun seedBookRequests() {
        val reqs = listOf(
            BookRequest(
                bookTitle = "Deep Work",
                author = "Cal Newport",
                category = "Business",
                maxPrice = 250,
                requesterName = "Riad Hasan",
                requesterLocation = "Dhanmondi, Dhaka",
                note = "Looking for a clean copy. Willing to pay cash or trade.",
                timestamp = "2 hours ago",
                status = "OPEN"
            )
        )
        for (r in reqs) {
            val docRef = bookRequestsRef.document()
            docRef.set(r.copy(id = docRef.id)).await()
        }
    }
}
