package com.example.boibinimoy.ui.sell

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.boibinimoy.R
import com.example.boibinimoy.data.BookRepository
import com.example.boibinimoy.data.FirestoreRepository
import com.example.boibinimoy.data.UserManager
import com.example.boibinimoy.databinding.ActivitySellBookBinding
import com.example.boibinimoy.model.Book
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class SellBookActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySellBookBinding
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            binding.ivUploadedCoverPreview.setImageURI(uri)
            binding.ivUploadedCoverPreview.visibility = View.VISIBLE
            binding.tvPhotoUploadedBadge.visibility = View.VISIBLE
            binding.layoutUploadPrompt.visibility = View.GONE
            Toast.makeText(this, "Book cover photo selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySellBookBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupSpinners()
        setupPhotoUpload()
        setupListingModeChips()
        setupPriceCalculation()
        setupSubmitButton()

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            binding.appBarLayout.setPadding(0, systemBars.top, 0, 0)
            binding.layoutPublishBar.setPadding(
                binding.layoutPublishBar.paddingLeft,
                binding.layoutPublishBar.paddingTop,
                binding.layoutPublishBar.paddingRight,
                navBars.bottom + 12
            )
            insets
        }
    }

    private fun setupSpinners() {
        val categories = arrayOf("Fiction", "Academic", "Business", "Comics", "Historical", "Islamic", "Poetry", "Science")
        val catAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        binding.spinnerSellCategory.adapter = catAdapter

        val conditions = arrayOf("Brand New", "Like New", "Good", "Acceptable")
        val condAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, conditions)
        binding.spinnerSellCondition.adapter = condAdapter
    }

    private fun setupPhotoUpload() {
        binding.cardUploadPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun setupListingModeChips() {
        binding.chipSellCash.setOnClickListener {
            updateListingModeUI(isCash = true, isExchange = false, isFree = false)
        }
        binding.chipSellExchange.setOnClickListener {
            updateListingModeUI(isCash = true, isExchange = true, isFree = false)
        }
        binding.chipSellFree.setOnClickListener {
            updateListingModeUI(isCash = false, isExchange = false, isFree = true)
        }
    }

    private fun updateListingModeUI(isCash: Boolean, isExchange: Boolean, isFree: Boolean) {
        if (isFree) {
            binding.layoutCashPricing.visibility = View.GONE
            binding.tvSellDiscountBadge.visibility = View.GONE
            binding.layoutFreeNotice.visibility = View.VISIBLE
            binding.layoutExchangeWish.visibility = View.GONE
            binding.btnSubmitBook.text = "Publish Free Book"
        } else if (isExchange) {
            binding.layoutCashPricing.visibility = View.VISIBLE
            binding.tvSellDiscountBadge.visibility = View.VISIBLE
            binding.layoutFreeNotice.visibility = View.GONE
            binding.layoutExchangeWish.visibility = View.VISIBLE
            binding.btnSubmitBook.text = "Publish for Exchange"
        } else {
            binding.layoutCashPricing.visibility = View.VISIBLE
            binding.tvSellDiscountBadge.visibility = View.VISIBLE
            binding.layoutFreeNotice.visibility = View.GONE
            binding.layoutExchangeWish.visibility = View.GONE
            binding.btnSubmitBook.text = "Publish Listing"
        }
    }

    private fun setupPriceCalculation() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateDiscount()
            }
        }
        binding.etSellPrice.addTextChangedListener(watcher)
        binding.etSellOriginalPrice.addTextChangedListener(watcher)
        calculateDiscount()
    }

    private fun calculateDiscount() {
        val price = binding.etSellPrice.text.toString().toIntOrNull() ?: 0
        val originalPrice = binding.etSellOriginalPrice.text.toString().toIntOrNull() ?: 0

        if (originalPrice > price && price > 0) {
            val discount = ((originalPrice - price) * 100) / originalPrice
            binding.tvSellDiscountBadge.visibility = View.VISIBLE
            binding.tvSellDiscountBadge.text = "$discount% OFF original retail price (Great value for buyers!)"
        } else if (originalPrice == 0 && price > 0) {
            binding.tvSellDiscountBadge.visibility = View.VISIBLE
            binding.tvSellDiscountBadge.text = "Fixed price listing"
        } else {
            binding.tvSellDiscountBadge.visibility = View.GONE
        }
    }

    private suspend fun uploadCoverImage(uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val imageBytes = baos.toByteArray()

            try {
                val fileName = "covers/${UUID.randomUUID()}.jpg"
                val storageRef = FirebaseStorage.getInstance().reference.child(fileName)
                storageRef.putBytes(imageBytes).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()
                return@withContext downloadUrl
            } catch (storageError: Exception) {
                // Fallback: save to internal app storage if Firebase Storage write rule or network is unavailable
                val localFile = File(filesDir, "cover_${System.currentTimeMillis()}.jpg")
                FileOutputStream(localFile).use { it.write(imageBytes) }
                return@withContext Uri.fromFile(localFile).toString()
            }
        } catch (e: Exception) {
            return@withContext uri.toString()
        }
    }

    private fun setupSubmitButton() {
        binding.btnSubmitBook.setOnClickListener {
            val title = binding.etSellTitle.text.toString().trim()
            val author = binding.etSellAuthor.text.toString().trim()
            val location = binding.etSellLocation.text.toString().trim()

            if (title.isEmpty()) {
                binding.etSellTitle.error = "Please enter book title"
                binding.etSellTitle.requestFocus()
                return@setOnClickListener
            }
            if (author.isEmpty()) {
                binding.etSellAuthor.error = "Please enter author name"
                binding.etSellAuthor.requestFocus()
                return@setOnClickListener
            }
            if (location.isEmpty()) {
                binding.etSellLocation.error = "Please enter your location"
                binding.etSellLocation.requestFocus()
                return@setOnClickListener
            }

            val isFree = binding.chipSellFree.isChecked
            val isExchange = binding.chipSellExchange.isChecked

            val price = if (isFree) 0 else (binding.etSellPrice.text.toString().toIntOrNull() ?: 0)
            val originalPrice = if (isFree) 0 else (binding.etSellOriginalPrice.text.toString().toIntOrNull() ?: price)

            val category = binding.spinnerSellCategory.selectedItem.toString()
            val condition = binding.spinnerSellCondition.selectedItem.toString()
            val exchangeWish = binding.etSellExchangeWish.text.toString().trim()
            val notes = binding.etSellDescription.text.toString().trim()

            val currentUser = UserManager.currentUser
            val sellerName = currentUser?.name ?: "Riad Hasan"
            val sellerId = currentUser?.id ?: "user_default"
            val bookId = "book_${System.currentTimeMillis()}"

            val desc = if (notes.isNotEmpty()) notes else "Listed by $sellerName on BoiBinimoy. In $condition condition, well-preserved."

            binding.btnSubmitBook.isEnabled = false
            binding.btnSubmitBook.text = "Publishing…"

            lifecycleScope.launch {
                val uploadedCoverUrl = if (selectedImageUri != null) {
                    uploadCoverImage(selectedImageUri!!)
                } else {
                    ""
                }

                val newBook = Book(
                    id = bookId,
                    title = title,
                    author = author,
                    price = price,
                    originalPrice = originalPrice,
                    coverResId = 0,
                    coverUrl = uploadedCoverUrl,
                    category = category,
                    condition = condition,
                    location = location,
                    sellerName = sellerName,
                    sellerId = sellerId,
                    sellerRating = 5.0,
                    sellerReviewCount = 1,
                    sellerResponseTime = "just now",
                    isSellerVerified = true,
                    isExchangeAvailable = isExchange,
                    exchangeLookingFor = if (isExchange && exchangeWish.isNotEmpty()) exchangeWish else "",
                    description = desc,
                    publisher = "Standard Edition",
                    language = "Bangla / English"
                )

                // Add locally immediately so it's instantly discoverable in search & home
                BookRepository.addBook(newBook)

                try {
                    FirestoreRepository.addBook(newBook)
                    Toast.makeText(
                        this@SellBookActivity,
                        "Book published successfully",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@SellBookActivity,
                        "Published locally (${e.message ?: "offline"})",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }
}
