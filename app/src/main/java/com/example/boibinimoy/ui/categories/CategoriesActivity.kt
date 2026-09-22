package com.example.boibinimoy.ui.categories

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.boibinimoy.MainActivity
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
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_SELECTED_CATEGORY, category.name)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        binding.rvAllCategories.layoutManager = GridLayoutManager(this, 3)
        binding.rvAllCategories.adapter = adapter
    }
}
