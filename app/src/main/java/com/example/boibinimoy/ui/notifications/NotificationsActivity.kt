package com.example.boibinimoy.ui.notifications

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.boibinimoy.data.BookRepository
import com.example.boibinimoy.data.FirestoreRepository
import com.example.boibinimoy.databinding.ActivityNotificationsBinding
import com.example.boibinimoy.model.NotificationType
import com.example.boibinimoy.ui.adapter.NotificationAdapter
import com.example.boibinimoy.ui.chat.ChatActivity
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val density = resources.displayMetrics.density
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            binding.layoutNotifHeader.setPadding(
                binding.layoutNotifHeader.paddingLeft,
                systemBars.top + (8 * density).toInt(),
                binding.layoutNotifHeader.paddingRight,
                (8 * density).toInt()
            )

            binding.rvNotifications.setPadding(
                binding.rvNotifications.paddingLeft,
                binding.rvNotifications.paddingTop,
                binding.rvNotifications.paddingRight,
                navBars.bottom + (16 * density).toInt()
            )
            insets
        }

        binding.btnNotifBack.setOnClickListener {
            finish()
        }

        val localNotifications = BookRepository.getNotifications()
        adapter = NotificationAdapter(localNotifications.toMutableList()) { notif ->
            when (notif.type) {
                NotificationType.MESSAGE.name, "MESSAGE" -> {
                    startActivity(Intent(this, ChatActivity::class.java))
                }
                else -> {
                    Toast.makeText(this, "${notif.title}\n${notif.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter
        updateEmptyState(localNotifications.size)

        observeFirestoreNotifications()
    }

    private fun updateEmptyState(count: Int) {
        if (count == 0) {
            binding.layoutNotifEmpty.visibility = android.view.View.VISIBLE
            binding.rvNotifications.visibility = android.view.View.GONE
        } else {
            binding.layoutNotifEmpty.visibility = android.view.View.GONE
            binding.rvNotifications.visibility = android.view.View.VISIBLE
        }
    }

    private fun observeFirestoreNotifications() {
        lifecycleScope.launch {
            FirestoreRepository.getNotifications()
                .catch { }
                .collect { notifications ->
                    adapter.updateData(notifications)
                    updateEmptyState(notifications.size)
                }
        }
    }
}
