package com.example.boibinimoy.data

import com.example.boibinimoy.R
import com.example.boibinimoy.model.*

object BookRepository {

    private val favoriteStates = mutableMapOf<String, Boolean>()

    private val categoryIconMap = mapOf(
        "academic" to R.drawable.ic_cat_academic,
        "fiction" to R.drawable.ic_cat_fiction,
        "historical" to R.drawable.ic_cat_history,
        "history" to R.drawable.ic_cat_history,
        "poetry" to R.drawable.ic_cat_poetry,
        "science" to R.drawable.ic_cat_science,
        "comics" to R.drawable.ic_cat_comics,
        "business" to R.drawable.ic_cat_business,
        "islamic" to R.drawable.ic_cat_islamic
    )

    private val categoryColorMap = mapOf(
        "academic" to R.color.cat_academic_bg,
        "fiction" to R.color.cat_fiction_bg,
        "historical" to R.color.cat_history_bg,
        "history" to R.color.cat_history_bg,
        "poetry" to R.color.cat_poetry_bg,
        "science" to R.color.cat_science_bg,
        "comics" to R.color.cat_comics_bg,
        "business" to R.color.cat_business_bg,
        "islamic" to R.color.cat_islamic_bg
    )

    fun resolveCategoryIcon(categoryName: String): Int {
        val key = categoryName.trim().lowercase()
        return categoryIconMap[key] ?: R.drawable.ic_cat_academic
    }

    fun resolveCategoryColor(categoryName: String): Int {
        val key = categoryName.trim().lowercase()
        return categoryColorMap[key] ?: R.color.primary_green_mint
    }

    private val categories = listOf(
        Category("cat_1", "Academic", R.drawable.ic_cat_academic, R.color.cat_academic_bg, 0),
        Category("cat_2", "Fiction", R.drawable.ic_cat_fiction, R.color.cat_fiction_bg, 0),
        Category("cat_3", "Historical", R.drawable.ic_cat_history, R.color.cat_history_bg, 0),
        Category("cat_4", "Poetry", R.drawable.ic_cat_poetry, R.color.cat_poetry_bg, 0),
        Category("cat_5", "Science", R.drawable.ic_cat_science, R.color.cat_science_bg, 0),
        Category("cat_6", "Comics", R.drawable.ic_cat_comics, R.color.cat_comics_bg, 0),
        Category("cat_7", "Business", R.drawable.ic_cat_business, R.color.cat_business_bg, 0),
        Category("cat_8", "Islamic", R.drawable.ic_cat_islamic, R.color.cat_islamic_bg, 0)
    )

    private val books = mutableListOf<Book>()
    private val cartItems = mutableListOf<CartItem>()
    private val exchangeRequests = mutableListOf<ExchangeRequest>()
    private val notifications = mutableListOf<NotificationItem>()
    private val chatMessages = mutableListOf<ChatMessage>()

    fun setBooks(newList: List<Book>) {
        books.clear()
        books.addAll(newList)
    }

    fun getCategories(): List<Category> = categories

    fun getAllBooks(): List<Book> = books

    fun getFeaturedBooks(): List<Book> = books.take(4)

    fun getRecentBooks(): List<Book> = books

    fun getBookById(id: String): Book? = books.find { it.id == id }

    fun searchBooks(query: String, category: String? = null, maxPrice: Int? = null, condition: String? = null): List<Book> {
        return books.filter { book ->
            val matchesQuery = query.isBlank() || 
                book.title.contains(query, ignoreCase = true) || 
                book.author.contains(query, ignoreCase = true) ||
                book.category.contains(query, ignoreCase = true)

            val matchesCategory = category.isNullOrBlank() || category.equals("All", ignoreCase = true) || book.category.equals(category, ignoreCase = true)
            val matchesPrice = maxPrice == null || book.price <= maxPrice
            val matchesCondition = condition.isNullOrBlank() || condition.equals("All", ignoreCase = true) || book.condition.equals(condition, ignoreCase = true)

            matchesQuery && matchesCategory && matchesPrice && matchesCondition
        }
    }

    fun toggleFavorite(bookId: String): Boolean {
        val book = books.find { it.id == bookId }
        val nextValue = !(favoriteStates[bookId] ?: book?.isFavorite ?: false)
        favoriteStates[bookId] = nextValue
        book?.isFavorite = nextValue
        return nextValue
    }

    fun isFavorite(book: Book): Boolean {
        return favoriteStates[book.id] ?: book.isFavorite
    }

    fun addBook(book: Book) {
        books.removeAll { it.id == book.id }
        books.add(0, book)
    }

    fun deleteBook(bookId: String) {
        books.removeAll { it.id == bookId }
    }

    fun markBookSold(bookId: String) {
        val index = books.indexOfFirst { it.id == bookId }
        if (index != -1) {
            val book = books[index]
            books[index] = book.copy(isSold = true)
        }
    }

    fun getCartItems(): MutableList<CartItem> = cartItems

    fun addToCart(book: Book) {
        val existing = cartItems.find { it.book.id == book.id }
        if (existing != null) {
            existing.quantity++
        } else {
            cartItems.add(CartItem(book, 1))
        }
    }

    fun removeFromCart(bookId: String) {
        cartItems.removeAll { it.book.id == bookId }
    }

    fun getExchangeRequests(): MutableList<ExchangeRequest> = exchangeRequests

    fun setExchangeRequests(newList: List<ExchangeRequest>) {
        exchangeRequests.clear()
        exchangeRequests.addAll(newList)
    }

    fun addExchangeRequest(req: ExchangeRequest) {
        exchangeRequests.removeAll { it.id == req.id }
        exchangeRequests.add(0, req)
    }

    fun updateExchangeStatus(requestId: String, newStatus: String) {
        val index = exchangeRequests.indexOfFirst { it.id == requestId }
        if (index != -1) {
            val req = exchangeRequests[index]
            exchangeRequests[index] = req.copy(status = newStatus)
        }
    }

    fun getNotifications(): List<NotificationItem> = notifications

    fun setNotifications(newList: List<NotificationItem>) {
        notifications.clear()
        notifications.addAll(newList)
    }

    fun addNotification(item: NotificationItem) {
        notifications.add(0, item)
    }

    fun getChatMessages(): MutableList<ChatMessage> = chatMessages

    fun sendChatMessage(text: String) {
        chatMessages.add(
            ChatMessage(
                id = "c_${System.currentTimeMillis()}",
                chatId = "demo_chat",
                senderName = "Riad Hasan",
                senderId = "me",
                message = text,
                timestamp = System.currentTimeMillis(),
                isMe = true
            )
        )
    }
}
