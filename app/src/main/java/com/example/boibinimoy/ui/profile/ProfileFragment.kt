package com.example.boibinimoy.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.boibinimoy.MainActivity
import com.example.boibinimoy.R
import com.example.boibinimoy.data.BookRepository
import com.example.boibinimoy.data.FirestoreRepository
import com.example.boibinimoy.data.UserManager
import com.example.boibinimoy.databinding.FragmentProfileBinding
import com.example.boibinimoy.model.NotificationItem
import com.example.boibinimoy.model.NotificationType
import com.example.boibinimoy.ui.notifications.NotificationsActivity
import com.example.boibinimoy.ui.sell.SellBookActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private var isAdminMode = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeUserProfile()
        setupListeners()
    }

    private fun observeUserProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            UserManager.currentUserFlow.collectLatest { profile ->
                val user = profile ?: UserManager.currentUser
                if (user != null) {
                    isAdminMode = user.isAdmin
                    binding.tvProfileName.text = user.name
                    binding.tvProfileMeta.text = "${user.location} • ${user.email}"
                    binding.tvStatListed.text = user.booksListed.toString()
                    binding.tvStatSold.text = user.booksSold.toString()
                    binding.tvStatSwapped.text = user.booksExchanged.toString()

                    binding.switchAdminMode.setOnCheckedChangeListener(null)
                    binding.switchAdminMode.isChecked = user.isAdmin
                    binding.switchAdminMode.setOnCheckedChangeListener { _, isChecked ->
                        updateAdminMode(isChecked)
                    }
                    updateAdminVisibility(user.isAdmin)
                }
            }
        }
    }

    private fun updateAdminMode(isChecked: Boolean) {
        val previousState = isAdminMode
        isAdminMode = isChecked
        updateAdminVisibility(isChecked)

        UserManager.setAdminMode(isChecked) {
            val modeText = if (isChecked) "Admin Mode Activated (Full Control)" else "Standard User Mode"
            Toast.makeText(requireContext(), modeText, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateAdminVisibility(isAdmin: Boolean) {
        binding.tvAdminBadge.visibility = if (isAdmin) View.VISIBLE else View.GONE
        binding.cardAdminPanel.visibility = if (isAdmin) View.VISIBLE else View.GONE
    }

    private fun setupListeners() {
        binding.switchAdminMode.setOnCheckedChangeListener { _, isChecked ->
            updateAdminMode(isChecked)
        }

        binding.btnAdminBroadcast.setOnClickListener {
            showBroadcastDialog()
        }

        binding.btnAdminManageBooks.setOnClickListener {
            showAdminManageBooksDialog()
        }

        binding.rowMyListings.setOnClickListener {
            startActivity(Intent(requireContext(), SellBookActivity::class.java))
        }

        binding.rowWishlist.setOnClickListener {
            (activity as? MainActivity)?.navigateToSearch()
            Toast.makeText(requireContext(), "Showing available books", Toast.LENGTH_SHORT).show()
        }

        binding.rowNotifications.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationsActivity::class.java))
        }


        binding.rowHelp.setOnClickListener {
            Toast.makeText(requireContext(), "BoiBinimoy Support Hotline: support@boibinimoy.com", Toast.LENGTH_LONG).show()
        }

        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out from BoiBinimoy?")
                .setPositiveButton("Log Out") { _, _ ->
                    UserManager.signOut(requireContext())
                    Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showBroadcastDialog() {
        val dialogView = layoutInflater.inflate(R.layout.layout_admin_broadcast_dialog, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.etAdminNotifTitle)
        val etMsg = dialogView.findViewById<TextInputEditText>(R.id.etAdminNotifMsg)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnAdminCancelNotif)
        val btnSend = dialogView.findViewById<MaterialButton>(R.id.btnAdminSendNotif)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSend.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val msg = etMsg.text.toString().trim()

            if (title.isEmpty() || msg.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in notification title & message", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSend.isEnabled = false
            btnSend.text = "Sending..."

            // Also add to local notifications immediately for instant feedback
            BookRepository.addNotification(
                NotificationItem(
                    id = "broadcast_${System.currentTimeMillis()}",
                    title = title,
                    message = msg,
                    timestamp = "Just now",
                    type = NotificationType.SYSTEM.name,
                    isRead = false
                )
            )

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    FirestoreRepository.sendBroadcastNotification(title, msg)
                    Toast.makeText(requireContext(), "Broadcast notification sent to all users!", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                } catch (e: Exception) {
                    // Even if Firestore network fails, local repository is updated
                    Toast.makeText(requireContext(), "Broadcast posted locally! (${e.message ?: "offline mode"})", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun showAdminManageBooksDialog() {
        val books = BookRepository.getAllBooks()
        val totalBooks = books.size
        val soldCount = books.count { it.isSold }
        val activeCount = totalBooks - soldCount

        AlertDialog.Builder(requireContext())
            .setTitle("Platform Books Overview (Admin)")
            .setMessage(
                "• Total Books in Catalog: $totalBooks\n" +
                "• Active Listings: $activeCount\n" +
                "• Sold Out Books: $soldCount\n\n" +
                "As Admin, you can open any book in the Search or Detail screen to Mark as Sold or Delete the listing permanently."
            )
            .setPositiveButton("Browse & Moderate Books") { _, _ ->
                (activity as? MainActivity)?.navigateToSearch()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
