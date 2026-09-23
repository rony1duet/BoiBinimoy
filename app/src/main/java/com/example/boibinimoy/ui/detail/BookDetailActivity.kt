package com.example.boibinimoy.ui.detail

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.boibinimoy.R
import com.example.boibinimoy.data.BookRepository
import com.example.boibinimoy.data.FirestoreRepository
import com.example.boibinimoy.data.UserManager
import com.example.boibinimoy.databinding.ActivityBookDetailBinding
import com.example.boibinimoy.model.Book
import com.example.boibinimoy.model.ExchangeRequest
import com.example.boibinimoy.ui.adapter.loadBookCover
import com.example.boibinimoy.ui.chat.ChatActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BookDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookDetailBinding

    companion object {
        const val EXTRA_BOOK = "extra_book"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBookDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()

        val book = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_BOOK, Book::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_BOOK) as? Book
        }

        if (book != null) {
            bindBookDetails(book)
            setupListeners(book)
            observeAdminRole()
        } else {
            finish()
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.root.setPadding(0, systemBars.top, 0, 0)

            // Adjust bottom bar padding to sit above gesture hint bar
            binding.layoutBottomBar.setPadding(
                binding.layoutBottomBar.paddingLeft,
                binding.layoutBottomBar.paddingTop,
                binding.layoutBottomBar.paddingRight,
                navBars.bottom + 10
            )
            insets
        }
    }

    private fun bindBookDetails(book: Book) {
        with(binding) {
            ivDetailCover.loadBookCover(book.coverUrl, book.coverResId)
            tvDetailTitle.text = book.title
            tvDetailAuthor.text = "by ${book.author}"
            tvDetailPrice.text = "৳ ${book.price}"
            tvDetailOriginalPrice.text = "৳ ${book.originalPrice}"
            tvDetailConditionBadge.text = "${book.condition} Condition"

            tvDetailCategory.text = book.category
            tvDetailLanguage.text = book.language
            tvDetailPageCount.text = book.pageCount.toString()

            tvSellerName.text = book.sellerName
            tvSellerLocation.text = "${book.location} • Active ${book.sellerResponseTime}"
            tvSellerRating.text = book.sellerRating.toString()

            tvDetailDescription.text = book.description

            if (book.isExchangeAvailable) {
                layoutExchangeInfo.visibility = View.VISIBLE
                tvDetailExchangeLookingFor.text = "Looking for: ${book.exchangeLookingFor}"
            } else {
                layoutExchangeInfo.visibility = View.GONE
            }

            if (book.isSold) {
                tvDetailConditionBadge.text = "SOLD OUT"
                btnBuyNow.isEnabled = false
                btnBuyNow.text = "Sold Out"
                btnProposeExchange.isEnabled = false
            }

            updateFavoriteIcon(book.isFavorite)
        }
    }

    private fun observeAdminRole() {
        lifecycleScope.launch {
            UserManager.currentUserFlow.collectLatest { user ->
                val isAdmin = user?.isAdmin ?: UserManager.isAdmin()
                binding.cardAdminBookControls.visibility = if (isAdmin) View.VISIBLE else View.GONE
            }
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
            val user = UserManager.currentUser
            val offered = BookRepository.getAllBooks().firstOrNull { it.id != book.id } ?: book
            val req = ExchangeRequest(
                id = "req_${System.currentTimeMillis()}",
                requestedBookId = book.id,
                requestedBookTitle = book.title,
                offeredBookId = offered.id,
                offeredBookTitle = offered.title,
                userName = user?.name?.ifBlank { "Reader" } ?: "Reader",
                userLocation = user?.location?.ifBlank { "Not specified" } ?: "Not specified",
                status = "PENDING",
                date = "Just now",
                message = "I would love to swap my '${offered.title}' for your '${book.title}'."
            )

            // Update local repository immediately
            BookRepository.addExchangeRequest(req)
            binding.btnProposeExchange.isEnabled = false

            lifecycleScope.launch {
                try {
                    FirestoreRepository.addExchangeRequest(req)
                    Toast.makeText(this@BookDetailActivity, "Exchange proposal sent to ${book.sellerName}!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this@BookDetailActivity, "Proposal recorded locally (${e.message ?: "offline"})", Toast.LENGTH_SHORT).show()
                } finally {
                    binding.btnProposeExchange.isEnabled = true
                }
            }
        }

        binding.btnMarkSold.setOnClickListener {
            BookRepository.markBookSold(book.id)
            binding.tvDetailConditionBadge.text = "SOLD OUT"
            binding.btnBuyNow.isEnabled = false
            binding.btnBuyNow.text = "Sold Out"
            binding.btnProposeExchange.isEnabled = false

            lifecycleScope.launch {
                try {
                    FirestoreRepository.markBookAsSold(book.id)
                    Toast.makeText(this@BookDetailActivity, "Book marked as SOLD by Admin", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@BookDetailActivity, "Marked as sold locally", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnDeleteBookListing.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Book Listing (Admin)")
                .setMessage("Are you sure you want to delete '${book.title}' from the platform?")
                .setPositiveButton("Delete") { _, _ ->
                    BookRepository.deleteBook(book.id)
                    lifecycleScope.launch {
                        try {
                            FirestoreRepository.deleteBook(book.id)
                        } catch (_: Exception) {}
                    }
                    Toast.makeText(this, "Book listing deleted by Admin", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnChatSeller.setOnClickListener {
            val chatIntent = Intent(this, ChatActivity::class.java).apply {
                putExtra("extra_chat_id", "chat_${book.id}")
                putExtra("extra_recipient_name", book.sellerName.ifBlank { "Seller" })
            }
            startActivity(chatIntent)
        }

        binding.btnCallSeller.setOnClickListener {
            lifecycleScope.launch {
                var phone = ""
                if (book.sellerId.isNotBlank()) {
                    try {
                        val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users").document(book.sellerId).get().await()
                        phone = doc.getString("phone") ?: ""
                    } catch (_: Exception) {}
                }
                if (phone.isNotBlank()) {
                    try {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                        startActivity(dialIntent)
                    } catch (_: Exception) {
                        Toast.makeText(this@BookDetailActivity, "Calling seller ($phone)...", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@BookDetailActivity, "Seller has not provided a phone number. Please use Chat!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
