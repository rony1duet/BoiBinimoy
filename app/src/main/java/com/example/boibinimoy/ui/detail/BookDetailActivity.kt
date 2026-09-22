package com.example.boibinimoy.ui.detail

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.example.boibinimoy.R
import com.example.boibinimoy.data.BookRepository
import com.example.boibinimoy.databinding.ActivityBookDetailBinding
import com.example.boibinimoy.model.Book
import com.example.boibinimoy.model.ExchangeRequest
import com.example.boibinimoy.ui.chat.ChatActivity

class BookDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BOOK = "extra_book"
    }

    private lateinit var binding: ActivityBookDetailBinding
    private var currentBook: Book? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentBook = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_BOOK, Book::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_BOOK) as? Book
        }
        if (currentBook == null) {
            finish()
            return
        }

        bindBookDetails(currentBook!!)
        setupListeners(currentBook!!)
    }

    private fun bindBookDetails(book: Book) {
        with(binding) {
            ivDetailCover.setImageResource(book.coverResId)
            tvDetailTitle.text = book.title
            tvDetailAuthor.text = "by ${book.author}"
            tvDetailPrice.text = "৳ ${book.price}"
            tvDetailOriginalPrice.text = "৳ ${book.originalPrice}"
            tvDetailConditionBadge.text = "${book.condition} Condition"

            tvDetailCategory.text = book.category
            tvDetailLanguage.text = book.language
            tvDetailPageCount.text = book.pageCount.toString()

            tvSellerName.text = book.sellerName
            tvSellerLocation.text = "📍 ${book.location} • Active ${book.sellerResponseTime}"
            tvSellerRating.text = book.sellerRating.toString()

            tvDetailDescription.text = book.description

            if (book.isExchangeAvailable) {
                layoutExchangeInfo.visibility = View.VISIBLE
                tvDetailExchangeLookingFor.text = "Looking for: ${book.exchangeLookingFor}"
            } else {
                layoutExchangeInfo.visibility = View.GONE
            }

            updateFavoriteIcon(book.isFavorite)
        }
    }

    private fun updateFavoriteIcon(isFav: Boolean) {
        binding.btnDetailFavorite.setImageResource(
            if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart
        )
    }

    private fun setupListeners(book: Book) {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnDetailFavorite.setOnClickListener {
            val isFav = BookRepository.toggleFavorite(book.id)
            book.isFavorite = isFav
            updateFavoriteIcon(isFav)
            val msg = if (isFav) "Added to wishlist" else "Removed from wishlist"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        binding.btnBuyNow.setOnClickListener {
            BookRepository.addToCart(book)
            Toast.makeText(this, "Added '${book.title}' to cart! ৳ ${book.price}", Toast.LENGTH_SHORT).show()
        }

        binding.btnProposeExchange.setOnClickListener {
            val offered = BookRepository.getAllBooks().firstOrNull { it.id != book.id } ?: book
            val req = ExchangeRequest(
                id = "req_${System.currentTimeMillis()}",
                requestedBookId = book.id,
                requestedBookTitle = book.title,
                offeredBookId = offered.id,
                offeredBookTitle = offered.title,
                userName = "Riad Hasan",
                userLocation = "Dhanmondi, Dhaka",
                status = "PENDING",
                date = "Just now",
                message = "I would love to swap my book for your '${book.title}'."
            )
            BookRepository.getExchangeRequests().add(0, req)
            Toast.makeText(this, "Exchange request sent to ${book.sellerName}!", Toast.LENGTH_LONG).show()
        }

        binding.btnChatSeller.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }

        binding.btnCallSeller.setOnClickListener {
            Toast.makeText(this, "Calling ${book.sellerName}...", Toast.LENGTH_SHORT).show()
        }
    }
}
