package com.example.boibinimoy.ui.notifications

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                    if (notifications.isNotEmpty()) {
                        adapter.updateData(notifications)
                    }
                    updateEmptyState(notifications.size.coerceAtLeast(BookRepository.getNotifications().size))
                }
        }
    }
}
