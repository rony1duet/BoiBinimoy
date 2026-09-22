package com.example.boibinimoy.ui.notifications

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.boibinimoy.data.BookRepository
import com.example.boibinimoy.databinding.ActivityNotificationsBinding
import com.example.boibinimoy.model.NotificationType
import com.example.boibinimoy.ui.adapter.NotificationAdapter
import com.example.boibinimoy.ui.chat.ChatActivity

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnNotifBack.setOnClickListener {
            finish()
        }

        val notifications = BookRepository.getNotifications()
        val adapter = NotificationAdapter(notifications) { notif ->
            when (notif.type) {
                NotificationType.MESSAGE.name, "MESSAGE" -> {
                    startActivity(Intent(this, ChatActivity::class.java))
                }
                else -> {
                    Toast.makeText(this, notif.title, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter
    }
}
