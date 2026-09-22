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
                title = "New Book Request Posted",
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
                title = "Order Placed Successfully!",
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

}
