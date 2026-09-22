package com.example.boibinimoy.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.boibinimoy.MainActivity
import com.example.boibinimoy.databinding.FragmentProfileBinding
import com.example.boibinimoy.ui.notifications.NotificationsActivity
import com.example.boibinimoy.ui.sell.SellBookActivity

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
