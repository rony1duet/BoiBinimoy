package com.example.boibinimoy.ui.categories

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.boibinimoy.data.BookRepository
import com.example.boibinimoy.databinding.ActivityCategoriesBinding
import com.example.boibinimoy.ui.adapter.CategoryAdapter

class CategoriesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoriesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCatBack.setOnClickListener {
            finish()
        }

        val categories = BookRepository.getCategories().toMutableList()
        val adapter = CategoryAdapter(categories) { category ->
            Toast.makeText(this, "Category: ${category.name} (${category.bookCount} books available)", Toast.LENGTH_SHORT).show()
        }

        binding.rvAllCategories.layoutManager = GridLayoutManager(this, 3)
        binding.rvAllCategories.adapter = adapter
    }
}
