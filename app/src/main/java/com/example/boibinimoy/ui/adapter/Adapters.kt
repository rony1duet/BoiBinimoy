package com.example.boibinimoy.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.boibinimoy.R
import com.example.boibinimoy.data.BookRepository
import com.example.boibinimoy.databinding.*
import com.example.boibinimoy.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun ImageView.loadBookCover(coverUrl: String?, coverResId: Int) {
    if (!coverUrl.isNullOrBlank()) {
        load(coverUrl) {
            crossfade(true)
            placeholder(if (coverResId != 0) coverResId else R.drawable.cover_generic)
            error(if (coverResId != 0) coverResId else R.drawable.cover_generic)
        }
    } else if (coverResId != 0) {
        setImageResource(coverResId)
    } else {
        setImageResource(R.drawable.cover_generic)
    }
}

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
        val categoryName = category.name.trim()
        val resolvedIcon = BookRepository.resolveCategoryIcon(categoryName)
        val resolvedColor = BookRepository.resolveCategoryColor(categoryName)

        holder.binding.tvCategoryName.text = categoryName
        val dynamicCount = BookRepository.getAllBooks().count { it.category.equals(categoryName, ignoreCase = true) }
        val countToShow = if (dynamicCount > 0) dynamicCount else category.bookCount
        if (countToShow > 0) {
            holder.binding.tvCategoryCount.visibility = View.VISIBLE
            holder.binding.tvCategoryCount.text = "$countToShow ${if (countToShow == 1) "book" else "books"}"
        } else {
            holder.binding.tvCategoryCount.visibility = View.GONE
        }
        holder.binding.ivCategoryIcon.setImageResource(resolvedIcon)
        holder.binding.ivCategoryIcon.contentDescription = categoryName
        holder.binding.ivCategoryIcon.setColorFilter(
            ContextCompat.getColor(holder.itemView.context, R.color.text_primary)
        )

        val cardColor = ContextCompat.getColor(holder.itemView.context, resolvedColor)
        holder.binding.cardCategory.setCardBackgroundColor(cardColor)

        val clickListener = View.OnClickListener {
            onCategoryClick(category)
        }
        holder.binding.root.setOnClickListener(clickListener)
        holder.binding.cardCategory.setOnClickListener(clickListener)
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
            book.isFavorite = BookRepository.isFavorite(book)
            ivBookCover.loadBookCover(book.coverUrl, book.coverResId)
            tvBookTitle.text = book.title
            tvBookAuthor.text = book.author
            tvBookPrice.text = "৳ ${book.price}"

            btnFavorite.setImageResource(
                if (book.isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart
            )

            btnFavorite.setOnClickListener {
                val currentPos = holder.bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    onFavoriteClick(book, currentPos)
                    notifyItemChanged(currentPos)
                }
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
            book.isFavorite = BookRepository.isFavorite(book)
            ivGridCover.loadBookCover(book.coverUrl, book.coverResId)
            tvGridTitle.text = book.title
            tvGridAuthor.text = book.author
            tvGridPrice.text = "৳ ${book.price}"
            tvGridCondition.text = book.condition
            tvGridLocation.text = book.location

            ivExchangeBadge.visibility = if (book.isExchangeAvailable) View.VISIBLE else View.GONE

            btnGridFavorite.setImageResource(
                if (book.isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart
            )

            btnGridFavorite.setOnClickListener {
                val currentPos = holder.bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    onFavoriteClick(book, currentPos)
                    notifyItemChanged(currentPos)
                }
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
            ivCartCover.loadBookCover(item.book.coverUrl, item.book.coverResId)
            tvCartTitle.text = item.book.title
            tvCartAuthor.text = item.book.author
            tvCartCondition.text = "Condition: ${item.book.condition}"
            tvCartPrice.text = "৳ ${item.book.price * item.quantity}"
            tvQuantity.text = item.quantity.toString()

            btnPlus.setOnClickListener {
                val currentPos = holder.bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    item.quantity++
                    notifyItemChanged(currentPos)
                    onQuantityChanged()
                }
            }

            btnMinus.setOnClickListener {
                val currentPos = holder.bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    if (item.quantity > 1) {
                        item.quantity--
                        notifyItemChanged(currentPos)
                        onQuantityChanged()
                    } else {
                        val removed = items.removeAt(currentPos)
                        notifyItemRemoved(currentPos)
                        notifyItemRangeChanged(currentPos, items.size - currentPos)
                        onItemRemoved(removed)
                        onQuantityChanged()
                    }
                }
            }

            btnRemoveCart.setOnClickListener {
                val currentPos = holder.bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    val removed = items.removeAt(currentPos)
                    notifyItemRemoved(currentPos)
                    notifyItemRangeChanged(currentPos, items.size - currentPos)
                    onItemRemoved(removed)
                    onQuantityChanged()
                }
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
            tvExchangeLocation.text = "${req.userLocation} • ${req.date}"
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
    private var isAdmin: Boolean,
    private val onOfferBook: (BookRequest) -> Unit,
    private val onDeleteRequest: (BookRequest) -> Unit
) : RecyclerView.Adapter<BookRequestAdapter.BookRequestViewHolder>() {

    inner class BookRequestViewHolder(val binding: ItemBookRequestBinding) : RecyclerView.ViewHolder(binding.root)

    fun updateData(newRequests: List<BookRequest>) {
        requests.clear()
        requests.addAll(newRequests)
        notifyDataSetChanged()
    }

    fun updateAdminState(isAdmin: Boolean) {
        this.isAdmin = isAdmin
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
            tvRequestUserLocation.text = "${req.requesterName} • ${req.requesterLocation}"
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

            // Dynamic icon and tint based on notification type
            val (iconRes, iconTintRes, bgTintRes) = when (notif.type.uppercase()) {
                "EXCHANGE" -> Triple(R.drawable.ic_swap_horiz, R.color.primary_green, R.color.primary_green_mint)
                "PRICE_DROP" -> Triple(R.drawable.ic_nav_cart, R.color.accent_orange, R.color.tag_condition_bg)
                "MESSAGE" -> Triple(R.drawable.ic_chat, R.color.primary_green_dark, R.color.primary_green_mint)
                "ORDER" -> Triple(R.drawable.ic_check_circle, R.color.primary_green, R.color.primary_green_container)
                "BOOK_REQUEST" -> Triple(R.drawable.ic_menu_book, R.color.cat_science_icon, R.color.cat_science_bg)
                else -> Triple(R.drawable.ic_bell, R.color.primary_green, R.color.primary_green_mint)
            }
            ivNotifIcon.setImageResource(iconRes)
            ivNotifIcon.setColorFilter(ContextCompat.getColor(root.context, iconTintRes))
            ivNotifIcon.backgroundTintList = ContextCompat.getColorStateList(root.context, bgTintRes)

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

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

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
        val formattedTime = if (msg.timestamp > 0) timeFormat.format(Date(msg.timestamp)) else "Just now"
        with(holder.binding) {
            if (msg.isMe) {
                layoutOutgoing.visibility = View.VISIBLE
                layoutIncoming.visibility = View.GONE
                tvOutgoingText.text = msg.message
                tvOutgoingTime.text = "$formattedTime • Delivered"
            } else {
                layoutIncoming.visibility = View.VISIBLE
                layoutOutgoing.visibility = View.GONE
                tvIncomingText.text = msg.message
                tvIncomingTime.text = formattedTime
            }
        }
    }

    override fun getItemCount(): Int = messages.size
}
