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
import com.example.boibinimoy.R
import com.example.boibinimoy.ui.notifications.NotificationsActivity
import com.example.boibinimoy.ui.sell.SellBookActivity
import com.example.boibinimoy.ui.adapter.HeroSlideAdapter
import com.example.boibinimoy.ui.adapter.HeroSlideItem
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var featuredAdapter: FeaturedBookAdapter? = null
    private var recentAdapter: FeaturedBookAdapter? = null
    private var categoryAdapter: CategoryAdapter? = null

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
        val slides = listOf(
            HeroSlideItem(
                title = "Give Books\nA New Life",
                subtitle = "Buy, sell and discover\ngreat books around you.",
                buttonText = "Sell Your Book",
                bgDrawableRes = R.drawable.bg_hero_card,
                onActionClick = {
                    startActivity(Intent(requireContext(), SellBookActivity::class.java))
                }
            ),
            HeroSlideItem(
                title = "100% Free Book\nExchange & Swap",
                subtitle = "Trade novels, academic text\n& storybooks at 0% fee.",
                buttonText = "Explore Swaps",
                bgDrawableRes = R.drawable.bg_hero_card_blue,
                onActionClick = {
                    (activity as? MainActivity)?.navigateToSearch()
                }
            ),
            HeroSlideItem(
                title = "Join 1,200+ Readers\nin Bangladesh",
                subtitle = "Find pre-loved reads from\nstudents & book lovers.",
                buttonText = "Browse Books",
                bgDrawableRes = R.drawable.bg_hero_card_purple,
                onActionClick = {
                    (activity as? MainActivity)?.navigateToSearch()
                }
            )
        )

        binding.vp2Hero.adapter = HeroSlideAdapter(slides)

        // Ensure smooth horizontal dragging inside NestedScrollView
        var startX = 0f
        var startY = 0f
        binding.vp2Hero.getChildAt(0)?.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    v.parent.requestDisallowInterceptTouchEvent(true)
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val dx = kotlin.math.abs(event.x - startX)
                    val dy = kotlin.math.abs(event.y - startY)
                    if (dx > dy && dx > 10f) {
                        v.parent.requestDisallowInterceptTouchEvent(true)
                    } else if (dy > dx && dy > 10f) {
                        v.parent.requestDisallowInterceptTouchEvent(false)
                    }
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }

        binding.vp2Hero.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateHeroDots(position)
            }
        })

        // Dot click support
        binding.btnDot1.setOnClickListener { binding.vp2Hero.setCurrentItem(0, true) }
        binding.btnDot2.setOnClickListener { binding.vp2Hero.setCurrentItem(1, true) }
        binding.btnDot3.setOnClickListener { binding.vp2Hero.setCurrentItem(2, true) }
    }

    private fun updateHeroDots(activeIndex: Int) {
        val density = resources.displayMetrics.density
        val activeWidth = (18 * density).toInt()
        val inactiveWidth = (6 * density).toInt()

        val dots = listOf(binding.dotHero1, binding.dotHero2, binding.dotHero3)
        dots.forEachIndexed { index, dot ->
            val isActive = index == activeIndex
            dot.setBackgroundResource(if (isActive) R.drawable.bg_carousel_dot_active else R.drawable.bg_carousel_dot_inactive)
            val params = dot.layoutParams
            params.width = if (isActive) activeWidth else inactiveWidth
            dot.layoutParams = params
        }
    }

    private fun setupCategoriesFromFirestore() {
        // Show local data immediately while Firestore loads
        val localCategories = BookRepository.getCategories()
        categoryAdapter = CategoryAdapter(localCategories.toMutableList()) { category ->
            (activity as? MainActivity)?.navigateToSearchWithCategory(category.name)
        }
        binding.rvCategories.adapter = categoryAdapter

        // Update with Firestore data
        viewLifecycleOwner.lifecycleScope.launch {
            FirestoreRepository.getCategories()
                .catch {
                    // Silently keep showing local data on error
                }
                .collect { firestoreCategories ->
                    if (firestoreCategories.isNotEmpty()) {
                        val firestoreCatsWithIcons = firestoreCategories.map { fsCat ->
                            val localMatch = localCategories.find { it.name.equals(fsCat.name, ignoreCase = true) }
                            val resolvedName = fsCat.name.ifBlank { localMatch?.name ?: "" }
                            val resolvedIcon = BookRepository.resolveCategoryIcon(resolvedName)
                            val resolvedColor = BookRepository.resolveCategoryColor(resolvedName)
                            fsCat.copy(
                                name = resolvedName,
                                iconResId = resolvedIcon,
                                backgroundColor = resolvedColor
                            )
                        }
                        categoryAdapter?.updateData(firestoreCatsWithIcons)
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
                val isFavorite = BookRepository.toggleFavorite(book.id)
                val msg = if (isFavorite) "Added ${book.title} to wishlist!" else "Removed ${book.title} from wishlist"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvFeaturedBooks.adapter = featuredAdapter

        recentAdapter = FeaturedBookAdapter(
            localRecent.toMutableList(),
            onBookClick = { openBookDetail(it) },
            onFavoriteClick = { book, _ ->
                val isFavorite = BookRepository.toggleFavorite(book.id)
                val msg = if (isFavorite) "Added ${book.title} to wishlist!" else "Removed ${book.title} from wishlist"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvRecentBooks.adapter = recentAdapter

        // Update with Firestore real-time data
        viewLifecycleOwner.lifecycleScope.launch {
            FirestoreRepository.getBooks()
                .catch { /* keep showing local data */ }
                .collect { books ->
                    BookRepository.setBooks(books)
                    val featured = books.take(4)
                    val recent = books.drop(4).take(6)

                    featuredAdapter?.updateData(featured)
                    recentAdapter?.updateData(if (recent.isEmpty()) books.take(4) else recent)
                    categoryAdapter?.notifyDataSetChanged()
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

    override fun onResume() {
        super.onResume()
        featuredAdapter?.updateData(BookRepository.getFeaturedBooks())
        recentAdapter?.updateData(BookRepository.getRecentBooks())
        categoryAdapter?.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
