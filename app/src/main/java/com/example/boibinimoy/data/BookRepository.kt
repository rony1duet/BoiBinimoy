package com.example.boibinimoy.data

import com.example.boibinimoy.R
import com.example.boibinimoy.model.*

object BookRepository {

    private val categories = listOf(
        Category("cat_1", "Academic", R.drawable.ic_cat_academic, R.color.cat_academic_bg, 340),
        Category("cat_2", "Fiction", R.drawable.ic_cat_fiction, R.color.cat_fiction_bg, 520),
        Category("cat_3", "Historical", R.drawable.ic_cat_history, R.color.cat_history_bg, 190),
        Category("cat_4", "Poetry", R.drawable.ic_cat_poetry, R.color.cat_poetry_bg, 145),
        Category("cat_5", "Science", R.drawable.ic_cat_science, R.color.cat_science_bg, 280),
        Category("cat_6", "Comics", R.drawable.ic_cat_comics, R.color.cat_comics_bg, 110),
        Category("cat_7", "Business", R.drawable.ic_cat_business, R.color.cat_business_bg, 310),
        Category("cat_8", "Islamic", R.drawable.ic_cat_islamic, R.color.cat_islamic_bg, 225)
    )

    private val books = mutableListOf(
        Book(
            id = "book_1",
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
            sellerReviewCount = 42,
            sellerResponseTime = "within 15 mins",
            isSellerVerified = true,
            isExchangeAvailable = true,
            exchangeLookingFor = "Sapiens or Deep Work",
            description = "The Alchemist follows the journey of an Andalusian shepherd boy named Santiago. Believing a recurring dream to be prophetic, he decides to travel to a Romani fortune-teller in a nearby town to discover its meaning.",
            publisher = "HarperOne",
            language = "English",
            publishedYear = "2014",
            pageCount = 208,
            isbn = "978-0062315007"
        ),
        Book(
            id = "book_2",
            title = "Sapiens",
            author = "Yuval Noah Harari",
            price = 300,
            originalPrice = 750,
            coverResId = R.drawable.cover_sapiens,
            category = "Historical",
            condition = "Good",
            location = "Mirpur 10, Dhaka",
            sellerName = "Nusrat Jahan",
            sellerRating = 4.8,
            sellerReviewCount = 29,
            sellerResponseTime = "within 1 hour",
            isSellerVerified = true,
            isExchangeAvailable = true,
            exchangeLookingFor = "Homo Deus or 21 Lessons",
            description = "A Brief History of Humankind surveys the history of humankind from the evolution of archaic human species in the Stone Age up to the twenty-first century.",
            publisher = "Harper",
            language = "English",
            publishedYear = "2015",
            pageCount = 498,
            isbn = "978-0062316097"
        ),
        Book(
            id = "book_3",
            title = "Rich Dad Poor Dad",
            author = "Robert Kiyosaki",
            price = 200,
            originalPrice = 450,
            coverResId = R.drawable.cover_rich_dad,
            category = "Business",
            condition = "Like New",
            location = "Uttara, Dhaka",
            sellerName = "Farhan Kabir",
            sellerRating = 5.0,
            sellerReviewCount = 56,
            sellerResponseTime = "within 30 mins",
            isSellerVerified = true,
            isExchangeAvailable = false,
            exchangeLookingFor = "Cash sale preferred",
            description = "Rich Dad Poor Dad is Robert's story of growing up with two dads — his real father and the father of his best friend, his rich dad — and the ways in which both men shaped his thoughts about money and investing.",
            publisher = "Plata Publishing",
            language = "English",
            publishedYear = "2017",
            pageCount = 336,
            isbn = "978-1612680194"
        ),
        Book(
            id = "book_4",
            title = "Atomic Habits",
            author = "James Clear",
            price = 280,
            originalPrice = 650,
            coverResId = R.drawable.cover_atomic_habits,
            category = "Business",
            condition = "Brand New",
            location = "Banani, Dhaka",
            sellerName = "Sadia Rahman",
            sellerRating = 4.9,
            sellerReviewCount = 67,
            sellerResponseTime = "within 10 mins",
            isSellerVerified = true,
            isExchangeAvailable = true,
            exchangeLookingFor = "Psychology of Money",
            description = "No matter your goals, Atomic Habits offers a proven framework for improving — every day. James Clear, one of the world's leading experts on habit formation, reveals practical strategies to form good habits.",
            publisher = "Avery",
            language = "English",
            publishedYear = "2018",
            pageCount = 320,
            isbn = "978-0735211292"
        ),
        Book(
            id = "book_5",
            title = "পদ্মানদীর মাঝি",
            author = "মানিক বন্দ্যোপাধ্যায়",
            price = 180,
            originalPrice = 350,
            coverResId = R.drawable.cover_padma_nadir,
            category = "Fiction",
            condition = "Like New",
            location = "Chittagong",
            sellerName = "Ahsan Habib",
            sellerRating = 4.7,
            sellerReviewCount = 18,
            sellerResponseTime = "within 2 hours",
            isSellerVerified = true,
            isExchangeAvailable = true,
            exchangeLookingFor = "লালসালু বা অন্য কোনো ক্লাসিক",
            description = "পদ্মানদীর মাঝি মানিক বন্দ্যোপাধ্যায় রচিত একটি কালজয়ী বাংলা উপন্যাস। পদ্মা তীরের জেলেদের জীবনসংগ্রাম ও জীবনের গভীর বাস্তব রূপায়িত হয়েছে এই অমর সৃষ্টিতে।",
            publisher = "ঐতিহ্য প্রকাশনী",
            language = "Bangla",
            publishedYear = "2020",
            pageCount = 184,
            isbn = "978-9847761008"
        ),
        Book(
            id = "book_6",
            title = "দেবী",
            author = "হুমায়ূন আহমেদ",
            price = 220,
            originalPrice = 400,
            coverResId = R.drawable.cover_debi,
            category = "Fiction",
            condition = "Good",
            location = "Sylhet",
            sellerName = "Mahmudul Hasan",
            sellerRating = 4.9,
            sellerReviewCount = 31,
            sellerResponseTime = "within 45 mins",
            isSellerVerified = true,
            isExchangeAvailable = true,
            exchangeLookingFor = "নিশীথিনী অথবা অন্য মিসির আলি উপন্যাস",
            description = "মিসির আলি চরিত্রের প্রথম উপন্যাস। রহস্য ও মনস্তাত্ত্বিক বিশ্লেষণের এক অনবদ্য মেলবন্ধন। রানু নামের এক বিচিত্র মানসিক অবস্থার তরুণীকে ঘিরে আবর্তিত কাহিনী।",
            publisher = "অন্যপ্রকাশ",
            language = "Bangla",
            publishedYear = "2019",
            pageCount = 160,
            isbn = "978-9845021234"
        )
    )

    private val cartItems = mutableListOf<CartItem>(
        CartItem(books[0], 1),
        CartItem(books[3], 1)
    )

    private val exchangeRequests = mutableListOf<ExchangeRequest>(
        ExchangeRequest(
            id = "req_1",
            requestedBookId = books[0].id,
            requestedBookTitle = books[0].title,
            offeredBookId = books[1].id,
            offeredBookTitle = books[1].title,
            userName = "Kazi Shakil",
            userLocation = "Bashundhara R/A, Dhaka",
            status = "PENDING",
            date = "Today, 4:30 PM",
            message = "Hi! I have Sapiens in great condition. Would love to swap for The Alchemist. Can meet at AIUB campus."
        ),
        ExchangeRequest(
            id = "req_2",
            requestedBookId = books[3].id,
            requestedBookTitle = books[3].title,
            offeredBookId = books[2].id,
            offeredBookTitle = books[2].title,
            userName = "Anika Tabassum",
            userLocation = "Dhanmondi 32, Dhaka",
            status = "ACCEPTED",
            date = "Yesterday",
            message = "Exchange agreed. Meeting tomorrow at Rabindra Sarobar."
        )
    )

    private val notifications = mutableListOf<NotificationItem>(
        NotificationItem(
            id = "notif_1",
            title = "New Exchange Proposal!",
            message = "Kazi Shakil offered 'Sapiens' in exchange for your 'The Alchemist'.",
            timestamp = "10 mins ago",
            type = NotificationType.EXCHANGE.name,
            isRead = false
        ),
        NotificationItem(
            id = "notif_2",
            title = "Price Drop Alert",
            message = "'Atomic Habits' by James Clear is now available for ৳ 280 in your area.",
            timestamp = "2 hours ago",
            type = NotificationType.PRICE_DROP.name,
            isRead = false
        ),
        NotificationItem(
            id = "notif_3",
            title = "Message from Tanvir",
            message = "Tanvir Ahmed: 'Yes, the book is available. When can you collect?'",
            timestamp = "5 hours ago",
            type = NotificationType.MESSAGE.name,
            isRead = true
        ),
        NotificationItem(
            id = "notif_4",
            title = "Order Confirmed #BB-9021",
            message = "Your order for 'Rich Dad Poor Dad' is being prepared by the seller.",
            timestamp = "1 day ago",
            type = NotificationType.ORDER.name,
            isRead = true
        )
    )

    private val chatMessages = mutableListOf<ChatMessage>(
        ChatMessage("c1", "demo_chat", "Tanvir Ahmed", "seller_001", "Assalamu Alaikum! Is The Alchemist still available for exchange?", System.currentTimeMillis() - 3600000, false),
        ChatMessage("c2", "demo_chat", "Riad Hasan", "me", "Walaikum Assalam! Yes, it's available and in pristine condition.", System.currentTimeMillis() - 3500000, true),
        ChatMessage("c3", "demo_chat", "Tanvir Ahmed", "seller_001", "Awesome! Would you like to swap with Sapiens? Can meet around Dhanmondi 27.", System.currentTimeMillis() - 3400000, false),
        ChatMessage("c4", "demo_chat", "Riad Hasan", "me", "That sounds perfect! Let's meet tomorrow at 5 PM.", System.currentTimeMillis() - 3300000, true)
    )

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
        book?.let {
            it.isFavorite = !it.isFavorite
            return it.isFavorite
        }
        return false
    }

    fun addBook(book: Book) {
        books.add(0, book)
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

    fun updateExchangeStatus(requestId: String, newStatus: String) {
        val index = exchangeRequests.indexOfFirst { it.id == requestId }
        if (index != -1) {
            val req = exchangeRequests[index]
            exchangeRequests[index] = req.copy(status = newStatus)
        }
    }

    fun getNotifications(): List<NotificationItem> = notifications

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
