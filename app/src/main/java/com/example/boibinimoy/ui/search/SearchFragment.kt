package com.example.boibinimoy.ui.search

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.boibinimoy.R
import com.example.boibinimoy.data.BookRepository
import com.example.boibinimoy.data.FirestoreRepository
import com.example.boibinimoy.databinding.FragmentSearchBinding
import com.example.boibinimoy.model.Book
import com.example.boibinimoy.ui.adapter.BookGridAdapter
import com.example.boibinimoy.ui.detail.BookDetailActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var bookGridAdapter: BookGridAdapter
    private var allBooksList: List<Book> = emptyList()

    private var currentCategory: String? = null
    private var currentMaxPrice: Int? = null
    private var currentCondition: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchInput()
        setupFilterChips()
        setupFilterButton()
        observeFirestoreBooks()
    }

    fun filterByCategory(categoryName: String) {
        currentCategory = categoryName
        when (categoryName) {
            "Fiction" -> binding.chipFiction.isChecked = true
            "Academic" -> binding.chipAcademic.isChecked = true
            "Business" -> binding.chipBusiness.isChecked = true
            "Historical" -> binding.chipHistorical.isChecked = true
            else -> binding.chipAll.isChecked = true
        }
        performSearch()
    }

    private fun setupRecyclerView() {
        allBooksList = BookRepository.getAllBooks()
        bookGridAdapter = BookGridAdapter(
            allBooksList,
            onBookClick = { book ->
                val intent = Intent(requireContext(), BookDetailActivity::class.java).apply {
                    putExtra(BookDetailActivity.EXTRA_BOOK, book)
                }
                startActivity(intent)
            },
            onFavoriteClick = { book, _ ->
                val isFav = BookRepository.toggleFavorite(book.id)
                val msg = if (isFav) "Added ${book.title} to wishlist!" else "Removed from wishlist"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvSearchResults.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvSearchResults.adapter = bookGridAdapter
    }

    private fun observeFirestoreBooks() {
        viewLifecycleOwner.lifecycleScope.launch {
            FirestoreRepository.getBooks()
                .catch { }
                .collect { books ->
                    if (books.isNotEmpty()) {
                        val localBooks = BookRepository.getAllBooks()
                        allBooksList = books.map { fsBook ->
                            val local = localBooks.find { it.title == fsBook.title }
                            fsBook.copy(coverResId = local?.coverResId ?: fsBook.coverResId)
                        }
                        performSearch()
                    }
                }
        }
    }

    private fun setupSearchInput() {
        binding.etSearchQuery.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnClearSearch.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                performSearch()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearchQuery.text.clear()
        }
    }

    private fun setupFilterChips() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            when (checkedIds.first()) {
                R.id.chipAll -> {
                    currentCategory = null
                    currentMaxPrice = null
                }
                R.id.chipFiction -> {
                    currentCategory = "Fiction"
                    currentMaxPrice = null
                }
                R.id.chipAcademic -> {
                    currentCategory = "Academic"
                    currentMaxPrice = null
                }
                R.id.chipBusiness -> {
                    currentCategory = "Business"
                    currentMaxPrice = null
                }
                R.id.chipHistorical -> {
                    currentCategory = "Historical"
                    currentMaxPrice = null
                }
                R.id.chipUnder200 -> {
                    currentCategory = null
                    currentMaxPrice = 200
                }
            }
            performSearch()
        }
    }

    private fun setupFilterButton() {
        binding.cardFilterToggle.setOnClickListener {
            showFilterBottomSheet()
        }
    }

    private fun showFilterBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.layout_filter_bottom_sheet, null)
        dialog.setContentView(sheetView)

        val slider = sheetView.findViewById<Slider>(R.id.sliderPrice)
        val tvPriceLabel = sheetView.findViewById<TextView>(R.id.tvMaxPriceLabel)
        val btnApply = sheetView.findViewById<MaterialButton>(R.id.btnApplyFilters)
        val tvReset = sheetView.findViewById<TextView>(R.id.tvResetFilter)
        val rbAll = sheetView.findViewById<RadioButton>(R.id.rbConditionAll)
        val rbLikeNew = sheetView.findViewById<RadioButton>(R.id.rbConditionLikeNew)
        val rbGood = sheetView.findViewById<RadioButton>(R.id.rbConditionGood)

        slider.value = (currentMaxPrice ?: 500).toFloat().coerceIn(100f, 1000f)
        tvPriceLabel.text = "Maximum Price: ৳ ${slider.value.toInt()}"

        slider.addOnChangeListener { _, value, _ ->
            tvPriceLabel.text = "Maximum Price: ৳ ${value.toInt()}"
        }

        btnApply.setOnClickListener {
            currentMaxPrice = slider.value.toInt()
            currentCondition = when {
                rbLikeNew.isChecked -> "Like New"
                rbGood.isChecked -> "Good"
                else -> null
            }
            dialog.dismiss()
            performSearch()
        }

        tvReset.setOnClickListener {
            currentMaxPrice = null
            currentCondition = null
            rbAll.isChecked = true
            slider.value = 500f
            dialog.dismiss()
            performSearch()
        }

        dialog.show()
    }

    private fun performSearch() {
        val query = binding.etSearchQuery.text.toString().trim()
        val results = allBooksList.filter { book ->
            val matchesQuery = query.isBlank() ||
                book.title.contains(query, ignoreCase = true) ||
                book.author.contains(query, ignoreCase = true) ||
                book.category.contains(query, ignoreCase = true)

            val matchesCategory = currentCategory.isNullOrBlank() || currentCategory.equals("All", ignoreCase = true) || book.category.equals(currentCategory, ignoreCase = true)
            val matchesPrice = currentMaxPrice == null || book.price <= currentMaxPrice!!
            val matchesCondition = currentCondition.isNullOrBlank() || currentCondition.equals("All", ignoreCase = true) || book.condition.equals(currentCondition, ignoreCase = true)

            matchesQuery && matchesCategory && matchesPrice && matchesCondition
        }

        bookGridAdapter.updateBooks(results)

        if (results.isEmpty()) {
            binding.rvSearchResults.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.tvResultsCount.text = "No books found"
        } else {
            binding.rvSearchResults.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
            binding.tvResultsCount.text = "Showing ${results.size} books"
        }
    }

    override fun onResume() {
        super.onResume()
        performSearch()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
