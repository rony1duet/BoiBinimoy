package com.example.boibinimoy.ui.chat

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
        enableEdgeToEdge()
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupChatRecycler()
        observeMessages()
        setupListeners()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            binding.layoutChatHeader.setPadding(
                binding.layoutChatHeader.paddingLeft,
                systemBars.top + 8,
                binding.layoutChatHeader.paddingRight,
                8
            )

            val bottomInset = if (ime.bottom > 0) ime.bottom else navBars.bottom
            binding.layoutChatInput.setPadding(
                binding.layoutChatInput.paddingLeft,
                binding.layoutChatInput.paddingTop,
                binding.layoutChatInput.paddingRight,
                bottomInset + 8
            )
            insets
        }
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
            try {
                val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                    data = android.net.Uri.parse("tel:+8801712345678")
                }
                startActivity(dialIntent)
            } catch (e: Exception) {
                val recipient = binding.tvChatRecipient.text.toString().ifBlank { "seller" }
                Toast.makeText(this, "Calling $recipient...", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnMakeOffer.setOnClickListener {
            binding.etChatMessage.setText("Would you consider an exchange or price adjustment?")
        }

        binding.chipQuick1.setOnClickListener {
            binding.etChatMessage.setText("Is this book still available?")
        }
        binding.chipQuick2.setOnClickListener {
            binding.etChatMessage.setText("Where would be a convenient meetup location?")
        }
        binding.chipQuick3.setOnClickListener {
            binding.etChatMessage.setText("Can you do ৳ 200 for this book?")
        }

        binding.btnSendMessage.setOnClickListener {
            val text = binding.etChatMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                binding.etChatMessage.text.clear()
                BookRepository.sendChatMessage(text)
                chatAdapter.updateMessages(BookRepository.getChatMessages())
                binding.rvChatMessages.scrollToPosition(BookRepository.getChatMessages().size - 1)

                val user = com.example.boibinimoy.data.UserManager.currentUser
                val senderName = user?.name ?: "You"
                val senderId = user?.id ?: "me"

                lifecycleScope.launch {
                    try {
                        FirestoreRepository.sendMessage(
                            chatId,
                            ChatMessage(
                                chatId = chatId,
                                senderName = senderName,
                                senderId = senderId,
                                message = text,
                                isMe = true
                            )
                        )
                    } catch (e: Exception) {
                        // Local message already saved and rendered in chat UI
                    }
                }
            }
        }
    }
}
