package com.example.boibinimoy

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.boibinimoy.databinding.ActivityMainBinding
import com.example.boibinimoy.ui.cart.CartFragment
import com.example.boibinimoy.ui.categories.CategoriesActivity
import com.example.boibinimoy.ui.home.HomeFragment
import com.example.boibinimoy.ui.notifications.NotificationsActivity
import com.example.boibinimoy.ui.profile.ProfileFragment
import com.example.boibinimoy.ui.search.SearchFragment
import com.example.boibinimoy.ui.sell.SellBookActivity

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SELECTED_CATEGORY = "extra_selected_category"
    }

    private lateinit var binding: ActivityMainBinding
    private val homeFragment = HomeFragment()
    private val searchFragment = SearchFragment()
    private val cartFragment = CartFragment()
    private val profileFragment = ProfileFragment()
    private var activeFragment: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupFragments()
        setupBottomNavigation()
        setupDrawerNavigation()
        handleIntent(intent)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainCoordinator) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            val density = resources.displayMetrics.density
            val baseNavHeightPx = (65 * density).toInt()

            // Padding bottom for bottomNavContainer so tab icons sit ABOVE the gesture bar
            binding.bottomNavContainer.setPadding(
                binding.bottomNavContainer.paddingLeft,
                (4 * density).toInt(),
                binding.bottomNavContainer.paddingRight,
                navBars.bottom + (4 * density).toInt()
            )

            val navParams = binding.bottomNavContainer.layoutParams
            navParams.height = baseNavHeightPx + navBars.bottom
            binding.bottomNavContainer.layoutParams = navParams

            // Set fragment container margin so fragment scrollable content is not obscured
            val fragmentParams = binding.fragmentContainer.layoutParams as ViewGroup.MarginLayoutParams
            fragmentParams.bottomMargin = navParams.height
            fragmentParams.topMargin = systemBars.top
            binding.fragmentContainer.layoutParams = fragmentParams

            insets
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val category = intent?.getStringExtra(EXTRA_SELECTED_CATEGORY)
        if (!category.isNullOrEmpty()) {
            navigateToSearchWithCategory(category)
        }
    }

    private fun setupFragments() {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, profileFragment, "PROFILE").hide(profileFragment)
            .add(R.id.fragmentContainer, cartFragment, "CART").hide(cartFragment)
            .add(R.id.fragmentContainer, searchFragment, "SEARCH").hide(searchFragment)
            .add(R.id.fragmentContainer, homeFragment, "HOME")
            .commit()
    }

    private fun setupBottomNavigation() {
        binding.navTabHome.setOnClickListener {
            switchFragment(homeFragment)
            highlightTab(binding.ivNavHome, binding.tvNavHome)
        }

        binding.navTabSearch.setOnClickListener {
            switchFragment(searchFragment)
            highlightTab(binding.ivNavSearch, binding.tvNavSearch)
        }

        binding.navTabSell.setOnClickListener {
            startActivity(Intent(this, SellBookActivity::class.java))
        }

        binding.navTabCart.setOnClickListener {
            switchFragment(cartFragment)
            highlightTab(binding.ivNavCart, binding.tvNavCart)
        }

        binding.navTabProfile.setOnClickListener {
            switchFragment(profileFragment)
            highlightTab(binding.ivNavProfile, binding.tvNavProfile)
        }
    }

    private fun switchFragment(target: Fragment) {
        if (activeFragment != target) {
            supportFragmentManager.beginTransaction()
                .hide(activeFragment)
                .show(target)
                .commit()
            activeFragment = target
        }
    }

    private fun highlightTab(activeIcon: ImageView, activeText: TextView) {
        val inactiveColor = ContextCompat.getColor(this, R.color.nav_inactive)
        val activeColor = ContextCompat.getColor(this, R.color.primary_green)

        binding.ivNavHome.setColorFilter(inactiveColor)
        binding.tvNavHome.setTextColor(inactiveColor)
        binding.tvNavHome.paint.isFakeBoldText = false

        binding.ivNavSearch.setColorFilter(inactiveColor)
        binding.tvNavSearch.setTextColor(inactiveColor)
        binding.tvNavSearch.paint.isFakeBoldText = false

        binding.ivNavCart.setColorFilter(inactiveColor)
        binding.tvNavCart.setTextColor(inactiveColor)
        binding.tvNavCart.paint.isFakeBoldText = false

        binding.ivNavProfile.setColorFilter(inactiveColor)
        binding.tvNavProfile.setTextColor(inactiveColor)
        binding.tvNavProfile.paint.isFakeBoldText = false

        activeIcon.setColorFilter(activeColor)
        activeText.setTextColor(activeColor)
        activeText.paint.isFakeBoldText = true
    }

    fun openDrawer() {
        binding.drawerLayout.openDrawer(GravityCompat.START)
    }

    fun navigateToSearch() {
        switchFragment(searchFragment)
        highlightTab(binding.ivNavSearch, binding.tvNavSearch)
    }

    fun navigateToSearchWithCategory(categoryName: String) {
        switchFragment(searchFragment)
        highlightTab(binding.ivNavSearch, binding.tvNavSearch)
        searchFragment.filterByCategory(categoryName)
    }

    private fun setupDrawerNavigation() {
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.menu_home -> {
                    switchFragment(homeFragment)
                    highlightTab(binding.ivNavHome, binding.tvNavHome)
                }
                R.id.menu_categories -> {
                    startActivity(Intent(this, CategoriesActivity::class.java))
                }
                R.id.menu_exchange_hub -> {
                    switchFragment(cartFragment)
                    highlightTab(binding.ivNavCart, binding.tvNavCart)
                }
                R.id.menu_notifications -> {
                    startActivity(Intent(this, NotificationsActivity::class.java))
                }
                R.id.menu_safety -> {
                    switchFragment(homeFragment)
                    highlightTab(binding.ivNavHome, binding.tvNavHome)
                }
            }
            true
        }
    }
}
