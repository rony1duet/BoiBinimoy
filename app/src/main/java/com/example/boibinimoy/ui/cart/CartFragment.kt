package com.example.boibinimoy.ui.cart

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.boibinimoy.R
import com.example.boibinimoy.data.BookRepository
import com.example.boibinimoy.databinding.FragmentCartBinding
import com.example.boibinimoy.ui.adapter.CartAdapter
import com.example.boibinimoy.ui.adapter.ExchangeRequestAdapter
import com.example.boibinimoy.ui.chat.ChatActivity

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    private lateinit var cartAdapter: CartAdapter
    private lateinit var exchangeAdapter: ExchangeRequestAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTabs()
        setupCartRecyclerView()
        setupExchangeRecyclerView()
        updateCartTotals()
    }

    private fun setupTabs() {
        binding.tabCart.setOnClickListener {
            binding.layoutCartContainer.visibility = View.VISIBLE
            binding.layoutSwapsContainer.visibility = View.GONE

            binding.tabCart.setBackgroundResource(R.drawable.bg_pill_button)
            binding.tabCart.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.primary_green)
            binding.tabCart.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

            binding.tabSwaps.background = null
            binding.tabSwaps.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }

        binding.tabSwaps.setOnClickListener {
            binding.layoutCartContainer.visibility = View.GONE
            binding.layoutSwapsContainer.visibility = View.VISIBLE

            binding.tabSwaps.setBackgroundResource(R.drawable.bg_pill_button)
            binding.tabSwaps.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.primary_green)
            binding.tabSwaps.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

            binding.tabCart.background = null
            binding.tabCart.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }
    }

    private fun setupCartRecyclerView() {
        val cartItems = BookRepository.getCartItems()
        cartAdapter = CartAdapter(
            cartItems,
            onQuantityChanged = {
                updateCartTotals()
            },
            onItemRemoved = { item ->
                Toast.makeText(requireContext(), "Removed ${item.book.title} from cart", Toast.LENGTH_SHORT).show()
                updateCartTotals()
            }
        )
        binding.rvCartItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCartItems.adapter = cartAdapter

        binding.btnCheckout.setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(requireContext(), "Your cart is empty!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Order Placed Successfully! Seller notified.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupExchangeRecyclerView() {
        val requests = BookRepository.getExchangeRequests()
        exchangeAdapter = ExchangeRequestAdapter(
            requests,
            onAccept = { req ->
                BookRepository.updateExchangeStatus(req.id, "ACCEPTED")
                exchangeAdapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "Exchange Accepted! Contact the seller to arrange meet.", Toast.LENGTH_SHORT).show()
            },
            onDecline = { req ->
                BookRepository.updateExchangeStatus(req.id, "REJECTED")
                exchangeAdapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "Exchange Proposal Declined", Toast.LENGTH_SHORT).show()
            },
            onChat = { _ ->
                startActivity(Intent(requireContext(), ChatActivity::class.java))
            }
        )
        binding.rvExchangeRequests.layoutManager = LinearLayoutManager(requireContext())
        binding.rvExchangeRequests.adapter = exchangeAdapter
    }

    private fun updateCartTotals() {
        val cartItems = BookRepository.getCartItems()
        val subtotal = cartItems.sumOf { it.book.price * it.quantity }
        val delivery = if (cartItems.isEmpty()) 0 else 50
        val total = subtotal + delivery

        binding.tvCartSubtotal.text = "৳ $subtotal"
        binding.tvCartTotal.text = "৳ $total"
        binding.tabCart.text = "🛒 Cart (${cartItems.size})"
    }

    override fun onResume() {
        super.onResume()
        cartAdapter.notifyDataSetChanged()
        exchangeAdapter.notifyDataSetChanged()
        updateCartTotals()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
