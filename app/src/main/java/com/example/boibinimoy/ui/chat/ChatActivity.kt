package com.example.boibinimoy.ui.chat

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.boibinimoy.data.BookRepository
import com.example.boibinimoy.data.FirestoreRepository
import com.example.boibinimoy.databinding.ActivityChatBinding
import com.example.boibinimoy.model.ChatMessage
import com.example.boibinimoy.ui.adapter.ChatAdapter
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatAdapter: ChatAdapter
    private val chatId = "demo_chat"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupChatRecycler()
        observeMessages()
        setupListeners()
    }

    private fun setupChatRecycler() {
        // Start with local data immediately
        val localMessages = BookRepository.getChatMessages()
        chatAdapter = ChatAdapter(localMessages.toMutableList())
        binding.rvChatMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvChatMessages.adapter = chatAdapter
    }

    private fun observeMessages() {
        // Listen to Firestore real-time messages
        lifecycleScope.launch {
            FirestoreRepository.getChatMessages(chatId)
                .catch { /* keep showing local */ }
                .collect { messages ->
                    if (messages.isNotEmpty()) {
                        chatAdapter.updateMessages(messages)
                        binding.rvChatMessages.scrollToPosition(messages.size - 1)
                    }
                }
        }
    }

    private fun setupListeners() {
        binding.btnChatBack.setOnClickListener { finish() }

        binding.btnChatCall.setOnClickListener {
            Toast.makeText(this, "Calling Tanvir Ahmed...", Toast.LENGTH_SHORT).show()
        }

        binding.btnMakeOffer.setOnClickListener {
            binding.etChatMessage.setText("Would you consider ৳ 220 or an exchange for Sapiens?")
        }

        binding.chipQuick1.setOnClickListener {
            binding.etChatMessage.setText("Is this book still available?")
        }
        binding.chipQuick2.setOnClickListener {
            binding.etChatMessage.setText("Can we meet at Dhanmondi 27 / TSC?")
        }
        binding.chipQuick3.setOnClickListener {
            binding.etChatMessage.setText("Can you do ৳ 200 for this book?")
        }

        binding.btnSendMessage.setOnClickListener {
            val text = binding.etChatMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                binding.etChatMessage.text.clear()
                lifecycleScope.launch {
                    try {
                        FirestoreRepository.sendMessage(
                            chatId,
                            ChatMessage(
                                chatId = chatId,
                                senderName = "You",
                                senderId = "me",
                                message = text,
                                isMe = true
                            )
                        )
                    } catch (e: Exception) {
                        Toast.makeText(this@ChatActivity, "Message failed to send", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
