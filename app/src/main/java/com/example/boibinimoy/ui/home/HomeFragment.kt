package com.example.boibinimoy.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.boibinimoy.MainActivity
import com.example.boibinimoy.data.BookRepository
import com.example.boibinimoy.data.FirestoreRepository
import com.example.boibinimoy.databinding.FragmentHomeBinding
import com.example.boibinimoy.model.Book
import com.example.boibinimoy.ui.adapter.CategoryAdapter
import com.example.boibinimoy.ui.adapter.FeaturedBookAdapter
import com.example.boibinimoy.ui.categories.CategoriesActivity
import com.example.boibinimoy.ui.detail.BookDetailActivity
import com.example.boibinimoy.ui.notifications.NotificationsActivity
import com.example.boibinimoy.ui.sell.SellBookActivity
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var featuredAdapter: FeaturedBookAdapter? = null
    private var recentAdapter: FeaturedBookAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeader()
        setupSearchBar()
        setupHeroBanner()
        setupCategoriesFromFirestore()
        setupBooksFromFirestore()
    }

    private fun setupHeader() {
        binding.btnMenu.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }
        binding.btnNotification.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationsActivity::class.java))
        }
    }

    private fun setupSearchBar() {
        binding.layoutSearchBar.setOnClickListener {
            (activity as? MainActivity)?.navigateToSearch()
        }
        binding.btnFilter.setOnClickListener {
            (activity as? MainActivity)?.navigateToSearch()
        }
    }

    private fun setupHeroBanner() {
        binding.btnHeroSell.setOnClickListener {
            startActivity(Intent(requireContext(), SellBookActivity::class.java))
        }
    }

    private fun setupCategoriesFromFirestore() {
        // Show local data immediately while Firestore loads
        val localCategories = BookRepository.getCategories()
        val adapter = CategoryAdapter(localCategories.toMutableList()) { category ->
            (activity as? MainActivity)?.navigateToSearchWithCategory(category.name)
        }
        binding.rvCategories.adapter = adapter

        // Update with Firestore data
        viewLifecycleOwner.lifecycleScope.launch {
            FirestoreRepository.getCategories()
                .catch {
                    // Silently keep showing local data on error
                }
                .collect { firestoreCategories ->
                    if (firestoreCategories.isNotEmpty()) {
                        val firestoreCatsWithIcons = firestoreCategories.map { fsCat ->
                            // Map icon from local fallback by name
                            val localMatch = localCategories.find { it.name == fsCat.name }
                            fsCat.copy(
                                iconResId = localMatch?.iconResId ?: fsCat.iconResId,
                                backgroundColor = localMatch?.backgroundColor ?: fsCat.backgroundColor
                            )
                        }
                        adapter.updateData(firestoreCatsWithIcons)
                    }
                }
        }

        binding.tvViewAllCategories.setOnClickListener {
            startActivity(Intent(requireContext(), CategoriesActivity::class.java))
        }
    }

    private fun setupBooksFromFirestore() {
        // Show local data immediately
        val localFeatured = BookRepository.getFeaturedBooks()
        val localRecent = BookRepository.getRecentBooks()

        featuredAdapter = FeaturedBookAdapter(
            localFeatured.toMutableList(),
            onBookClick = { openBookDetail(it) },
            onFavoriteClick = { book, _ ->
                val msg = "Added ${book.title} to wishlist!"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvFeaturedBooks.adapter = featuredAdapter

        recentAdapter = FeaturedBookAdapter(
            localRecent.toMutableList(),
            onBookClick = { openBookDetail(it) },
            onFavoriteClick = { book, _ ->
                Toast.makeText(requireContext(), "Added ${book.title} to wishlist!", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvRecentBooks.adapter = recentAdapter

        // Update with Firestore real-time data
        viewLifecycleOwner.lifecycleScope.launch {
            FirestoreRepository.getBooks()
                .catch { /* keep showing local data */ }
                .collect { books ->
                    if (books.isNotEmpty()) {
                        // Use local cover drawable as fallback based on title matching
                        val booksWithCovers = books.map { book ->
                            val local = localFeatured.find { it.title == book.title }
                                ?: localRecent.find { it.title == book.title }
                            book.copy(coverResId = local?.coverResId ?: book.coverResId)
                        }
                        // Split: first 4 featured, rest recent
                        val featured = booksWithCovers.take(4)
                        val recent = booksWithCovers.drop(4).take(6)

                        featuredAdapter?.updateData(featured)
                        recentAdapter?.updateData(if (recent.isEmpty()) booksWithCovers.take(4) else recent)
                    }
                }
        }

        binding.tvViewAllFeatured.setOnClickListener {
            (activity as? MainActivity)?.navigateToSearch()
        }
        binding.tvViewAllRecent.setOnClickListener {
            (activity as? MainActivity)?.navigateToSearch()
        }
    }

    private fun openBookDetail(book: Book) {
        startActivity(
            Intent(requireContext(), BookDetailActivity::class.java).apply {
                putExtra(BookDetailActivity.EXTRA_BOOK, book)
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
