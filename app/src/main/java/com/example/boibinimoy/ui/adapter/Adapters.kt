package com.example.boibinimoy.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.boibinimoy.R
import com.example.boibinimoy.databinding.*
import com.example.boibinimoy.model.*

// 1. Category Adapter
class CategoryAdapter(
    private val categories: MutableList<Category>,
    private val onCategoryClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.binding.tvCategoryName.text = category.name
        if (category.iconResId != 0) {
            holder.binding.ivCategoryIcon.setImageResource(category.iconResId)
        }
        holder.binding.root.setOnClickListener {
            onCategoryClick(category)
        }
    }

    override fun getItemCount(): Int = categories.size

    fun updateData(newCategories: List<Category>) {
        categories.clear()
        categories.addAll(newCategories)
        notifyDataSetChanged()
    }
}

// 2. Featured Books Adapter (Matches Screenshot)
class FeaturedBookAdapter(
    private val books: MutableList<Book>,
    private val onBookClick: (Book) -> Unit,
    private val onFavoriteClick: (Book, Int) -> Unit
) : RecyclerView.Adapter<FeaturedBookAdapter.FeaturedBookViewHolder>() {

    inner class FeaturedBookViewHolder(val binding: ItemFeaturedBookBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeaturedBookViewHolder {
        val binding = ItemFeaturedBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeaturedBookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeaturedBookViewHolder, position: Int) {
        val book = books[position]
        with(holder.binding) {
            if (book.coverResId != 0) ivBookCover.setImageResource(book.coverResId)
            tvBookTitle.text = book.title
            tvBookAuthor.text = book.author
            tvBookPrice.text = "৳ ${book.price}"

            btnFavorite.setImageResource(
                if (book.isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart
            )

            btnFavorite.setOnClickListener {
                onFavoriteClick(book, position)
                notifyItemChanged(position)
            }

            root.setOnClickListener {
                onBookClick(book)
            }
        }
    }

    override fun getItemCount(): Int = books.size

    fun updateData(newBooks: List<Book>) {
        books.clear()
        books.addAll(newBooks)
        notifyDataSetChanged()
    }
}

// 3. Book Grid Adapter (For Search & Explorer)
class BookGridAdapter(
    private var books: List<Book>,
    private val onBookClick: (Book) -> Unit,
    private val onFavoriteClick: (Book, Int) -> Unit
) : RecyclerView.Adapter<BookGridAdapter.BookGridViewHolder>() {

    inner class BookGridViewHolder(val binding: ItemBookGridBinding) : RecyclerView.ViewHolder(binding.root)

    fun updateBooks(newBooks: List<Book>) {
        books = newBooks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookGridViewHolder {
        val binding = ItemBookGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookGridViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookGridViewHolder, position: Int) {
        val book = books[position]
        with(holder.binding) {
            ivGridCover.setImageResource(book.coverResId)
            tvGridTitle.text = book.title
            tvGridAuthor.text = book.author
            tvGridPrice.text = "৳ ${book.price}"
            tvGridCondition.text = book.condition
            tvGridLocation.text = "📍 ${book.location}"

            ivExchangeBadge.visibility = if (book.isExchangeAvailable) View.VISIBLE else View.GONE

            btnGridFavorite.setImageResource(
                if (book.isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart
            )

            btnGridFavorite.setOnClickListener {
                onFavoriteClick(book, position)
                notifyItemChanged(position)
            }

            root.setOnClickListener {
                onBookClick(book)
            }
        }
    }

    override fun getItemCount(): Int = books.size
}

// 4. Cart Items Adapter
class CartAdapter(
    private val items: MutableList<CartItem>,
    private val onQuantityChanged: () -> Unit,
    private val onItemRemoved: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(val binding: ItemCartBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            ivCartCover.setImageResource(item.book.coverResId)
            tvCartTitle.text = item.book.title
            tvCartAuthor.text = item.book.author
            tvCartCondition.text = "Condition: ${item.book.condition}"
            tvCartPrice.text = "৳ ${item.book.price * item.quantity}"
            tvQuantity.text = item.quantity.toString()

            btnPlus.setOnClickListener {
                item.quantity++
                notifyItemChanged(position)
                onQuantityChanged()
            }

            btnMinus.setOnClickListener {
                if (item.quantity > 1) {
                    item.quantity--
                    notifyItemChanged(position)
                    onQuantityChanged()
                } else {
                    val removed = items.removeAt(position)
                    notifyItemRemoved(position)
                    onItemRemoved(removed)
                    onQuantityChanged()
                }
            }

            btnRemoveCart.setOnClickListener {
                val removed = items.removeAt(position)
                notifyItemRemoved(position)
                onItemRemoved(removed)
                onQuantityChanged()
            }
        }
    }

    override fun getItemCount(): Int = items.size
}

// 5. Exchange Requests Adapter
class ExchangeRequestAdapter(
    private val requests: MutableList<ExchangeRequest>,
    private val onAccept: (ExchangeRequest) -> Unit,
    private val onDecline: (ExchangeRequest) -> Unit,
    private val onChat: (ExchangeRequest) -> Unit
) : RecyclerView.Adapter<ExchangeRequestAdapter.ExchangeViewHolder>() {

    inner class ExchangeViewHolder(val binding: ItemExchangeRequestBinding) : RecyclerView.ViewHolder(binding.root)

    fun updateData(newRequests: List<ExchangeRequest>) {
        requests.clear()
        requests.addAll(newRequests)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExchangeViewHolder {
        val binding = ItemExchangeRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExchangeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExchangeViewHolder, position: Int) {
        val req = requests[position]
        with(holder.binding) {
            tvExchangeUser.text = req.userName
            tvExchangeLocation.text = "📍 ${req.userLocation} • ${req.date}"
            tvExchangeStatus.text = req.status

            tvOfferedTitle.text = req.offeredBookTitle
            tvRequestedTitle.text = req.requestedBookTitle

            tvExchangeMessage.text = "\"${req.message}\""

            when (req.status.uppercase()) {
                "ACCEPTED" -> {
                    tvExchangeStatus.setBackgroundResource(R.drawable.bg_pill_button)
                    tvExchangeStatus.backgroundTintList = ContextCompat.getColorStateList(root.context, R.color.primary_green_mint)
                    tvExchangeStatus.setTextColor(ContextCompat.getColor(root.context, R.color.primary_green))
                    layoutActions.visibility = View.GONE
                }
                "REJECTED" -> {
                    tvExchangeStatus.setTextColor(ContextCompat.getColor(root.context, R.color.heart_red))
                    layoutActions.visibility = View.GONE
                }
                else -> {
                    layoutActions.visibility = View.VISIBLE
                }
            }

            btnAccept.setOnClickListener { onAccept(req) }
            btnDecline.setOnClickListener { onDecline(req) }
            btnChatProposal.setOnClickListener { onChat(req) }
        }
    }

    override fun getItemCount(): Int = requests.size
}

// 6. Book Requests Adapter (User Request a Book feature)
class BookRequestAdapter(
    private val requests: MutableList<BookRequest>,
    private val isAdmin: Boolean,
    private val onOfferBook: (BookRequest) -> Unit,
    private val onDeleteRequest: (BookRequest) -> Unit
) : RecyclerView.Adapter<BookRequestAdapter.BookRequestViewHolder>() {

    inner class BookRequestViewHolder(val binding: ItemBookRequestBinding) : RecyclerView.ViewHolder(binding.root)

    fun updateData(newRequests: List<BookRequest>) {
        requests.clear()
        requests.addAll(newRequests)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookRequestViewHolder {
        val binding = ItemBookRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookRequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookRequestViewHolder, position: Int) {
        val req = requests[position]
        with(holder.binding) {
            tvRequestTitle.text = "Wanted: ${req.bookTitle}"
            tvRequestAuthorCategory.text = "by ${req.author} • Category: ${req.category}"
            tvRequestUserLocation.text = "👤 ${req.requesterName} • 📍 ${req.requesterLocation}"
            tvRequestPrice.text = "Budget: ~৳ ${req.maxPrice}"
            tvRequestNote.text = if (req.note.isNotBlank()) "\"${req.note}\"" else "\"Looking for this book. Please contact me if you have it!\""
            tvRequestBadge.text = req.status

            btnDeleteRequest.visibility = if (isAdmin) View.VISIBLE else View.GONE
            btnDeleteRequest.setOnClickListener { onDeleteRequest(req) }

            btnOfferBook.setOnClickListener { onOfferBook(req) }
        }
    }

    override fun getItemCount(): Int = requests.size
}

// 7. Notifications Adapter
class NotificationAdapter(
    private val notifications: MutableList<NotificationItem>,
    private val onNotificationClick: (NotificationItem) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotifViewHolder>() {

    inner class NotifViewHolder(val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root)

    fun updateData(newNotifs: List<NotificationItem>) {
        notifications.clear()
        notifications.addAll(newNotifs)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotifViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotifViewHolder, position: Int) {
        val notif = notifications[position]
        with(holder.binding) {
            tvNotifTitle.text = notif.title
            tvNotifDesc.text = notif.message
            tvNotifTime.text = notif.timestamp

            viewUnreadDot.visibility = if (notif.isRead) View.GONE else View.VISIBLE

            root.setOnClickListener {
                notif.isRead = true
                viewUnreadDot.visibility = View.GONE
                onNotificationClick(notif)
            }
        }
    }

    override fun getItemCount(): Int = notifications.size
}

// 8. Chat Messages Adapter
class ChatAdapter(
    private val messages: MutableList<ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root)

    fun updateMessages(newMessages: List<ChatMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val msg = messages[position]
        with(holder.binding) {
            if (msg.isMe) {
                layoutOutgoing.visibility = View.VISIBLE
                layoutIncoming.visibility = View.GONE
                tvOutgoingText.text = msg.message
                tvOutgoingTime.text = "${msg.timestamp} • Delivered"
            } else {
                layoutIncoming.visibility = View.VISIBLE
                layoutOutgoing.visibility = View.GONE
                tvIncomingText.text = msg.message
                tvIncomingTime.text = msg.timestamp.toString()
            }
        }
    }

    override fun getItemCount(): Int = messages.size
}
