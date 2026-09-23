package com.example.boibinimoy.ui.categories

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.boibinimoy.MainActivity
import com.example.boibinimoy.data.BookRepository
import com.example.boibinimoy.data.FirestoreRepository
import com.example.boibinimoy.databinding.ActivityCategoriesBinding
import com.example.boibinimoy.model.Category
import com.example.boibinimoy.ui.adapter.CategoryAdapter
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class CategoriesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoriesBinding
    private lateinit var adapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()

        binding.btnCatBack.setOnClickListener {
            finish()
        }

        val localCategories = BookRepository.getCategories().toMutableList()
        adapter = CategoryAdapter(localCategories) { category ->
            openCategoryInSearch(category.name)
        }

        binding.rvAllCategories.layoutManager = GridLayoutManager(this, 3)
        binding.rvAllCategories.adapter = adapter

        observeFirestoreCategories(localCategories)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val density = resources.displayMetrics.density

            binding.layoutCatHeader.setPadding(
                binding.layoutCatHeader.paddingLeft,
                systemBars.top + (8 * density).toInt(),
                binding.layoutCatHeader.paddingRight,
                (8 * density).toInt()
            )

            binding.rvAllCategories.setPadding(
                binding.rvAllCategories.paddingLeft,
                binding.rvAllCategories.paddingTop,
                binding.rvAllCategories.paddingRight,
                navBars.bottom + (24 * density).toInt()
            )
            insets
        }
    }

    private fun observeFirestoreCategories(localCategories: List<Category>) {
        lifecycleScope.launch {
            FirestoreRepository.getCategories()
                .catch { }
                .collect { firestoreCategories ->
                    if (firestoreCategories.isNotEmpty()) {
                        val merged = firestoreCategories.map { fsCat ->
                            val local = localCategories.find { it.name.equals(fsCat.name, ignoreCase = true) }
                            val resolvedName = fsCat.name.ifBlank { local?.name ?: "" }
                            val resolvedIcon = BookRepository.resolveCategoryIcon(resolvedName)
                            val resolvedColor = BookRepository.resolveCategoryColor(resolvedName)
                            fsCat.copy(
                                name = resolvedName,
                                iconResId = resolvedIcon,
                                backgroundColor = resolvedColor
                            )
                        }
                        adapter.updateData(merged)
                    }
                }
        }
    }

    private fun openCategoryInSearch(categoryName: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_SELECTED_CATEGORY, categoryName)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }
}
