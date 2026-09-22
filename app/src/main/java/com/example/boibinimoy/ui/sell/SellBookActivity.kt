package com.example.boibinimoy.ui.sell

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.boibinimoy.R
import com.example.boibinimoy.data.FirestoreRepository
import com.example.boibinimoy.databinding.ActivitySellBookBinding
import com.example.boibinimoy.model.Book
import kotlinx.coroutines.launch

class SellBookActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySellBookBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySellBookBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinners()
        setupListeners()
    }

    private fun setupSpinners() {
        val categories = listOf("Fiction", "Academic", "Business", "Historical", "Science", "Poetry", "Comics", "Islamic")
        binding.spinnerSellCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        val conditions = listOf("Brand New", "Like New", "Good", "Acceptable")
        binding.spinnerSellCondition.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, conditions)
    }

    private fun setupListeners() {
        binding.btnSellBack.setOnClickListener { finish() }

        binding.cardUploadCover.setOnClickListener {
            Toast.makeText(this, "Book photo captured successfully!", Toast.LENGTH_SHORT).show()
        }

        binding.btnSubmitBook.setOnClickListener {
            val title = binding.etSellTitle.text.toString().trim()
            val author = binding.etSellAuthor.text.toString().trim()
            val priceStr = binding.etSellPrice.text.toString().trim()
            val location = binding.etSellLocation.text.toString().trim()

            if (title.isEmpty() || author.isEmpty() || location.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields (*)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val price = priceStr.toIntOrNull() ?: 200
            val originalPrice = binding.etSellOriginalPrice.text.toString().toIntOrNull() ?: (price * 2)
            val category = binding.spinnerSellCategory.selectedItem.toString()
            val condition = binding.spinnerSellCondition.selectedItem.toString()
            val isExchange = binding.chipSellExchange.isChecked
            val exchangeWish = binding.etSellExchangeWish.text.toString()

            val newBook = Book(
                title = title,
                author = author,
                price = price,
                originalPrice = originalPrice,
                coverResId = R.drawable.cover_alchemist,
                category = category,
                condition = condition,
                location = location,
                sellerName = "Riad Hasan",
                sellerRating = 5.0,
                sellerReviewCount = 1,
                sellerResponseTime = "just now",
                isSellerVerified = true,
                isExchangeAvailable = isExchange,
                exchangeLookingFor = if (exchangeWish.isNotEmpty()) exchangeWish else "Any good book",
                description = "Listed by user on BoiBinimoy. In $condition condition, well-preserved.",
                publisher = "Standard Edition",
                language = "Bangla / English"
            )

            // Show loading
            binding.btnSubmitBook.isEnabled = false
            binding.btnSubmitBook.text = "Publishing…"

            lifecycleScope.launch {
                try {
                    val docId = FirestoreRepository.addBook(newBook)
                    Toast.makeText(
                        this@SellBookActivity,
                        "Book listed on Firestore! ID: ${docId.take(8)}...",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } catch (e: Exception) {
                    binding.btnSubmitBook.isEnabled = true
                    binding.btnSubmitBook.text = "Publish Listing"
                    Toast.makeText(this@SellBookActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
