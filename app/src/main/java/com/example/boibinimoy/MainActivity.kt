package com.example.boibinimoy

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.boibinimoy.data.UserManager
import com.example.boibinimoy.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import com.example.boibinimoy.ui.cart.CartFragment
import com.example.boibinimoy.ui.categories.CategoriesActivity
import com.example.boibinimoy.ui.home.HomeFragment
import com.example.boibinimoy.ui.notifications.NotificationsActivity
import com.example.boibinimoy.ui.profile.ProfileFragment
import com.example.boibinimoy.ui.search.SearchFragment
import com.example.boibinimoy.ui.sell.SellBookActivity
import java.util.ArrayDeque

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

    private val fragmentBackStack = ArrayDeque<Fragment>()
    private var backPressedTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!UserManager.isLoggedIn()) {
            startActivity(Intent(this, com.example.boibinimoy.ui.auth.LoginActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupFragments()
        setupBottomNavigation()
        setupDrawerNavigation()
        setupBackPressHandler()
        observeDrawerUser()
        binding.navigationView.setCheckedItem(R.id.menu_home)
        handleIntent(intent)
    }

    private fun observeDrawerUser() {
        val headerView = binding.navigationView.getHeaderView(0)
        val tvName = headerView?.findViewById<TextView>(R.id.tvDrawerUserName)
        val tvEmail = headerView?.findViewById<TextView>(R.id.tvDrawerUserEmail)
        val tvAdminBadge = headerView?.findViewById<TextView>(R.id.tvDrawerAdminBadge)

        lifecycleScope.launch {
            UserManager.currentUserFlow.collect { profile ->
                val user = profile ?: UserManager.currentUser
                if (user != null) {
                    tvName?.text = user.name.ifBlank { "Reader" }
                    tvEmail?.text = user.email.ifBlank { "reader@boibinimoy.com" }
                    tvAdminBadge?.visibility = if (user.isAdmin) android.view.View.VISIBLE else android.view.View.GONE
                }
            }
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainCoordinator) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            val density = resources.displayMetrics.density
            val baseNavHeightPx = (64 * density).toInt()
            val navPaddingBottom = navBars.bottom + (6 * density).toInt()
            val topInset = systemBars.top

            binding.bottomNavContainer.setPadding(
                binding.bottomNavContainer.paddingLeft,
                (4 * density).toInt(),
                binding.bottomNavContainer.paddingRight,
                navPaddingBottom
            )

            val navParams = binding.bottomNavContainer.layoutParams
            navParams.height = baseNavHeightPx + navBars.bottom
            binding.bottomNavContainer.layoutParams = navParams

            val fragmentParams = binding.fragmentContainer.layoutParams as ViewGroup.MarginLayoutParams
            fragmentParams.bottomMargin = navParams.height
            fragmentParams.topMargin = topInset
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
        }

        binding.navTabSearch.setOnClickListener {
            switchFragment(searchFragment)
        }

        binding.navTabSell.setOnClickListener {
            startActivity(Intent(this, SellBookActivity::class.java))
        }

        binding.navTabCart.setOnClickListener {
            switchFragment(cartFragment)
        }

        binding.navTabProfile.setOnClickListener {
            switchFragment(profileFragment)
        }
    }

    private fun switchFragment(target: Fragment, addToBackStack: Boolean = true) {
        if (activeFragment != target) {
            if (addToBackStack) {
                if (fragmentBackStack.isEmpty() || fragmentBackStack.last != activeFragment) {
                    fragmentBackStack.addLast(activeFragment)
                }
            }
            supportFragmentManager.beginTransaction()
                .hide(activeFragment)
                .show(target)
                .commit()
            activeFragment = target
            syncTabHighlight(target)
        }
    }

    private fun syncTabHighlight(target: Fragment) {
        when (target) {
            homeFragment -> {
                highlightTab(binding.ivNavHome, binding.tvNavHome)
                binding.navigationView.setCheckedItem(R.id.menu_home)
            }
            searchFragment -> {
                highlightTab(binding.ivNavSearch, binding.tvNavSearch)
            }
            cartFragment -> {
                highlightTab(binding.ivNavCart, binding.tvNavCart)
                binding.navigationView.setCheckedItem(R.id.menu_exchange_hub)
            }
            profileFragment -> {
                highlightTab(binding.ivNavProfile, binding.tvNavProfile)
                binding.navigationView.setCheckedItem(R.id.menu_profile)
            }
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
    }

    fun navigateToSearchWithCategory(categoryName: String) {
        switchFragment(searchFragment)
        searchFragment.filterByCategory(categoryName)
    }

    private fun setupDrawerNavigation() {
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.menu_home -> {
                    switchFragment(homeFragment)
                }
                R.id.menu_profile -> {
                    switchFragment(profileFragment)
                }
                R.id.menu_categories -> {
                    startActivity(Intent(this, CategoriesActivity::class.java))
                }
                R.id.menu_exchange_hub -> {
                    switchFragment(cartFragment)
                }
                R.id.menu_notifications -> {
                    startActivity(Intent(this, NotificationsActivity::class.java))
                }
                R.id.menu_logout -> {
                    UserManager.signOut(this)
                }
            }
            menuItem.isChecked = true
            true
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 1. If navigation drawer is open, close it
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    return
                }

                // 2. Fragment-specific back press handling
                if (activeFragment == searchFragment && searchFragment.handleBackPress()) {
                    return
                }
                if (activeFragment == cartFragment && cartFragment.handleBackPress()) {
                    return
                }

                // 3. Navigate back through fragment history if available
                while (fragmentBackStack.isNotEmpty()) {
                    val prevFragment = fragmentBackStack.removeLast()
                    if (prevFragment != activeFragment) {
                        switchFragment(prevFragment, addToBackStack = false)
                        return
                    }
                }

                // 4. If on another tab but stack is empty, return to Home tab
                if (activeFragment != homeFragment) {
                    switchFragment(homeFragment, addToBackStack = false)
                    return
                }

                // 5. On Home tab: double back press within 2000ms to exit app
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    finish()
                } else {
                    backPressedTime = System.currentTimeMillis()
                    Toast.makeText(
                        this@MainActivity,
                        "Press back again to exit BoiBinimoy",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }
}
