package com.example.boibinimoy.ui.cart

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.boibinimoy.R
import com.example.boibinimoy.data.BookRepository
import com.example.boibinimoy.data.FirestoreRepository
import com.example.boibinimoy.data.UserManager
import com.example.boibinimoy.databinding.FragmentCartBinding
import com.example.boibinimoy.model.BookRequest
import com.example.boibinimoy.model.OrderItem
import com.example.boibinimoy.ui.adapter.BookRequestAdapter
import com.example.boibinimoy.ui.adapter.CartAdapter
import com.example.boibinimoy.ui.adapter.ExchangeRequestAdapter
import com.example.boibinimoy.ui.chat.ChatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    private lateinit var cartAdapter: CartAdapter
    private lateinit var exchangeAdapter: ExchangeRequestAdapter
    private lateinit var bookRequestAdapter: BookRequestAdapter

    private var isAdminMode = false

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
        setupBookRequestsRecyclerView()
        observeUserProfile()
        observeFirestoreData()
        updateCartTotals()
    }

    private fun setupTabs() {
        binding.tabCart.setOnClickListener {
            showContainer(binding.layoutCartContainer, binding.tabCart)
        }

        binding.tabSwaps.setOnClickListener {
            showContainer(binding.layoutSwapsContainer, binding.tabSwaps)
        }

        binding.tabRequests.setOnClickListener {
            showContainer(binding.layoutRequestsContainer, binding.tabRequests)
        }
    }

    private fun showContainer(targetContainer: View, activeTab: View) {
        binding.layoutCartContainer.visibility = if (targetContainer == binding.layoutCartContainer) View.VISIBLE else View.GONE
        binding.layoutSwapsContainer.visibility = if (targetContainer == binding.layoutSwapsContainer) View.VISIBLE else View.GONE
        binding.layoutRequestsContainer.visibility = if (targetContainer == binding.layoutRequestsContainer) View.VISIBLE else View.GONE

        val resetTab = { tab: View ->
            tab.background = null
            (tab as? TextView)?.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }

        resetTab(binding.tabCart)
        resetTab(binding.tabSwaps)
        resetTab(binding.tabRequests)

        activeTab.setBackgroundResource(R.drawable.bg_pill_button)
        activeTab.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.primary_green)
        (activeTab as? TextView)?.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
    }

    private fun observeUserProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            UserManager.currentUserFlow.collect { user ->
                val admin = user?.isAdmin ?: UserManager.isAdmin()
                isAdminMode = admin
                bookRequestAdapter.updateAdminState(admin)
            }
        }
    }

    private fun setupCartRecyclerView() {
        val cartItems = BookRepository.getCartItems()
        cartAdapter = CartAdapter(
            cartItems,
            onQuantityChanged = { updateCartTotals() },
            onItemRemoved = { item ->
                Toast.makeText(requireContext(), "Removed ${item.book.title} from cart", Toast.LENGTH_SHORT).show()
                updateCartTotals()
            }
        )
        binding.rvCartItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCartItems.adapter = cartAdapter

        binding.btnEmptyExploreBooks.setOnClickListener {
            (activity as? com.example.boibinimoy.MainActivity)?.navigateToSearch()
        }

        binding.btnCheckout.setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(requireContext(), "Your cart is empty!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val total = cartItems.sumOf { it.book.price * it.quantity } + 50
            binding.btnCheckout.isEnabled = false
            binding.btnCheckout.text = "Placing Order..."

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val user = UserManager.currentUser
                    val order = OrderItem(
                        items = cartItems.toList(),
                        totalAmount = total,
                        orderDate = "Just now",
                        status = "CONFIRMED",
                        userName = user?.name ?: "Riad Hasan",
                        deliveryAddress = user?.location ?: "Dhanmondi, Dhaka"
                    )
                    FirestoreRepository.addOrder(order)
                    cartItems.clear()
                    cartAdapter.notifyDataSetChanged()
                    updateCartTotals()
                    binding.btnCheckout.isEnabled = true
                    binding.btnCheckout.text = "Proceed to Checkout (bKash / COD)"
                    Toast.makeText(requireContext(), "Order Placed Successfully! Order confirmation saved.", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    binding.btnCheckout.isEnabled = true
                    binding.btnCheckout.text = "Proceed to Checkout (bKash / COD)"
                    Toast.makeText(requireContext(), "Error placing order: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupExchangeRecyclerView() {
        val requests = BookRepository.getExchangeRequests()
        exchangeAdapter = ExchangeRequestAdapter(
            requests,
            onAccept = { req ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        FirestoreRepository.updateExchangeStatus(req.id, "ACCEPTED")
                    } catch (_: Exception) {}
                }
                BookRepository.updateExchangeStatus(req.id, "ACCEPTED")
                exchangeAdapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "Exchange Accepted! Contact the seller to arrange meet.", Toast.LENGTH_SHORT).show()
            },
            onDecline = { req ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        FirestoreRepository.updateExchangeStatus(req.id, "REJECTED")
                    } catch (_: Exception) {}
                }
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
        updateSwapsVisibility(requests.size)
    }

    private fun setupBookRequestsRecyclerView() {
        bookRequestAdapter = BookRequestAdapter(
            mutableListOf(),
            isAdmin = isAdminMode,
            onOfferBook = { req ->
                startActivity(Intent(requireContext(), ChatActivity::class.java))
                Toast.makeText(requireContext(), "Opening chat with ${req.requesterName}", Toast.LENGTH_SHORT).show()
            },
            onDeleteRequest = { req ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        FirestoreRepository.deleteBookRequest(req.id)
                        Toast.makeText(requireContext(), "Request deleted by Admin", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error deleting: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        binding.rvBookRequests.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBookRequests.adapter = bookRequestAdapter
        updateRequestsVisibility(0)

        binding.btnPostBookRequest.setOnClickListener {
            showPostBookRequestDialog()
        }
    }

    private fun observeFirestoreData() {
        // Exchange requests stream
        viewLifecycleOwner.lifecycleScope.launch {
            FirestoreRepository.getExchangeRequests()
                .catch { }
                .collect { firestoreRequests ->
                    if (firestoreRequests.isNotEmpty()) {
                        exchangeAdapter.updateData(firestoreRequests)
                    }
                    updateSwapsVisibility(firestoreRequests.size.coerceAtLeast(BookRepository.getExchangeRequests().size))
                }
        }

        // Book requests stream
        viewLifecycleOwner.lifecycleScope.launch {
            FirestoreRepository.getBookRequests()
                .catch { }
                .collect { requests ->
                    bookRequestAdapter.updateData(requests)
                    updateRequestsVisibility(requests.size)
                }
        }
    }

    private fun showPostBookRequestDialog() {
        val dialogView = layoutInflater.inflate(R.layout.layout_request_book_dialog, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.etReqTitle)
        val etAuthor = dialogView.findViewById<TextInputEditText>(R.id.etReqAuthor)
        val etBudget = dialogView.findViewById<TextInputEditText>(R.id.etReqBudget)
        val etNote = dialogView.findViewById<TextInputEditText>(R.id.etReqNote)
        val spinnerCat = dialogView.findViewById<Spinner>(R.id.spinnerReqCategory)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancelRequest)
        val btnSubmit = dialogView.findViewById<MaterialButton>(R.id.btnSubmitRequest)

        val categories = listOf("Fiction", "Academic", "Business", "Historical", "Science", "Poetry", "Islamic")
        spinnerCat.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSubmit.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val author = etAuthor.text.toString().trim()
            val budget = etBudget.text.toString().toIntOrNull() ?: 250
            val note = etNote.text.toString().trim()
            val category = spinnerCat.selectedItem.toString()

            if (title.isEmpty() || author.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter book title and author", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSubmit.isEnabled = false
            btnSubmit.text = "Submitting..."

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val user = UserManager.currentUser
                    val newReq = BookRequest(
                        bookTitle = title,
                        author = author,
                        category = category,
                        maxPrice = budget,
                        requesterName = user?.name ?: "Riad Hasan",
                        requesterLocation = user?.location ?: "Dhanmondi, Dhaka",
                        note = note,
                        timestamp = "Just now",
                        status = "OPEN"
                    )
                    FirestoreRepository.addBookRequest(newReq)
                    Toast.makeText(requireContext(), "Book Request Posted! Sellers will notify you.", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                } catch (e: Exception) {
                    btnSubmit.isEnabled = true
                    btnSubmit.text = "Submit Request"
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun updateSwapsVisibility(count: Int) {
        if (count == 0) {
            binding.layoutSwapsEmpty.visibility = View.VISIBLE
            binding.rvExchangeRequests.visibility = View.GONE
        } else {
            binding.layoutSwapsEmpty.visibility = View.GONE
            binding.rvExchangeRequests.visibility = View.VISIBLE
        }
    }

    private fun updateRequestsVisibility(count: Int) {
        if (count == 0) {
            binding.layoutRequestsEmpty.visibility = View.VISIBLE
            binding.rvBookRequests.visibility = View.GONE
        } else {
            binding.layoutRequestsEmpty.visibility = View.GONE
            binding.rvBookRequests.visibility = View.VISIBLE
        }
    }

    private fun updateCartTotals() {
        val cartItems = BookRepository.getCartItems()
        if (cartItems.isEmpty()) {
            binding.layoutCartEmpty.visibility = View.VISIBLE
            binding.scrollCartContent.visibility = View.GONE
            binding.layoutCheckoutBar.visibility = View.GONE
        } else {
            binding.layoutCartEmpty.visibility = View.GONE
            binding.scrollCartContent.visibility = View.VISIBLE
            binding.layoutCheckoutBar.visibility = View.VISIBLE
        }

        val subtotal = cartItems.sumOf { it.book.price * it.quantity }
        val delivery = if (cartItems.isEmpty()) 0 else 50
        val total = subtotal + delivery

        binding.tvCartSubtotal.text = "৳ $subtotal"
        binding.tvDeliveryFee.text = "৳ $delivery"
        binding.tvCartTotal.text = "৳ $total"
        binding.tabCart.text = if (cartItems.isEmpty()) "Cart" else "Cart (${cartItems.size})"
    }

    override fun onResume() {
        super.onResume()
        cartAdapter.notifyDataSetChanged()
        exchangeAdapter.notifyDataSetChanged()
        updateCartTotals()
        updateSwapsVisibility(exchangeAdapter.itemCount)
        updateRequestsVisibility(bookRequestAdapter.itemCount)
    }

    fun handleBackPress(): Boolean {
        if (_binding != null && (binding.layoutSwapsContainer.visibility == View.VISIBLE || binding.layoutRequestsContainer.visibility == View.VISIBLE)) {
            showContainer(binding.layoutCartContainer, binding.tabCart)
            return true
        }
        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
