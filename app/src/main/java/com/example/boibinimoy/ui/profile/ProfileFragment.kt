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
import com.example.boibinimoy.data.FirestoreRepository
import com.example.boibinimoy.databinding.FragmentProfileBinding
import com.example.boibinimoy.ui.notifications.NotificationsActivity
import com.example.boibinimoy.ui.sell.SellBookActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.catch
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
            FirestoreRepository.getUserProfile("user_default")
                .catch { }
                .collect { profile ->
                    isAdminMode = profile.isAdmin
                    binding.tvProfileName.text = profile.name
                    binding.tvProfileMeta.text = "${profile.location} • Member since ${profile.memberSince}"
                    binding.tvStatListed.text = profile.booksListed.toString()
                    binding.tvStatSold.text = profile.booksSold.toString()
                    binding.tvStatSwapped.text = profile.booksExchanged.toString()

                    binding.switchAdminMode.setOnCheckedChangeListener(null)
                    binding.switchAdminMode.isChecked = profile.isAdmin
                    binding.switchAdminMode.setOnCheckedChangeListener { _, isChecked ->
                        updateAdminMode(isChecked)
                    }
                    updateAdminVisibility(profile.isAdmin)
                }
        }
    }

    private fun updateAdminMode(isChecked: Boolean) {
        val previousState = isAdminMode
        isAdminMode = isChecked
        updateAdminVisibility(isChecked)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                FirestoreRepository.toggleAdminRole("user_default", isChecked)
                val modeText = if (isChecked) "Admin Mode Activated" else "Standard User Mode"
                Toast.makeText(requireContext(), modeText, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                isAdminMode = previousState
                binding.switchAdminMode.setOnCheckedChangeListener(null)
                binding.switchAdminMode.isChecked = previousState
                binding.switchAdminMode.setOnCheckedChangeListener { _, checked ->
                    updateAdminMode(checked)
                }
                updateAdminVisibility(previousState)
                Toast.makeText(requireContext(), "Error updating role: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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

        binding.rowLanguage.setOnClickListener {
            Toast.makeText(requireContext(), "Language changed to English / বাংলা", Toast.LENGTH_SHORT).show()
        }

        binding.rowHelp.setOnClickListener {
            Toast.makeText(requireContext(), "BoiBinimoy Support Hotline: support@boibinimoy.com", Toast.LENGTH_LONG).show()
        }

        binding.btnLogout.setOnClickListener {
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
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

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    FirestoreRepository.sendBroadcastNotification(title, msg)
                    Toast.makeText(requireContext(), "Broadcast notification sent to all users!", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                } catch (e: Exception) {
                    btnSend.isEnabled = true
                    btnSend.text = "Send Broadcast"
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
