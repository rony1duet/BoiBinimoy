package com.example.boibinimoy.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.boibinimoy.MainActivity
import com.example.boibinimoy.R
import com.example.boibinimoy.data.UserManager
import com.example.boibinimoy.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var isSignUpMode = false
    private var backPressedTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupModeSwitch()
        setupListeners()
        setupBackPressHandler()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isSignUpMode) {
                    isSignUpMode = false
                    updateModeUi()
                    return
                }

                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    finish()
                } else {
                    backPressedTime = System.currentTimeMillis()
                    Toast.makeText(this@LoginActivity, "Press back again to exit BoiBinimoy", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.layoutLoginRoot.setPadding(
                binding.layoutLoginRoot.paddingLeft,
                systemBars.top + 24,
                binding.layoutLoginRoot.paddingRight,
                systemBars.bottom + 24
            )
            insets
        }
    }

    private fun setupModeSwitch() {
        binding.tabSignIn.setOnClickListener {
            if (isSignUpMode) {
                isSignUpMode = false
                updateModeUi()
            }
        }

        binding.tabSignUp.setOnClickListener {
            if (!isSignUpMode) {
                isSignUpMode = true
                updateModeUi()
            }
        }
    }

    private fun updateModeUi() {
        val activeBg = ContextCompat.getDrawable(this, R.drawable.bg_pill_button)
        val greenTint = ContextCompat.getColorStateList(this, R.color.primary_green)

        if (isSignUpMode) {
            binding.tabSignUp.background = activeBg
            binding.tabSignUp.backgroundTintList = greenTint
            binding.tabSignUp.setTextColor(ContextCompat.getColor(this, R.color.white))

            binding.tabSignIn.background = null
            binding.tabSignIn.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))

            binding.tilName.visibility = View.VISIBLE
            binding.tilLocation.visibility = View.VISIBLE
            binding.tilPhone.visibility = View.VISIBLE

            binding.btnAuthSubmit.text = "Create Account"
        } else {
            binding.tabSignIn.background = activeBg
            binding.tabSignIn.backgroundTintList = greenTint
            binding.tabSignIn.setTextColor(ContextCompat.getColor(this, R.color.white))

            binding.tabSignUp.background = null
            binding.tabSignUp.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))

            binding.tilName.visibility = View.GONE
            binding.tilLocation.visibility = View.GONE
            binding.tilPhone.visibility = View.GONE

            binding.btnAuthSubmit.text = "Sign In"
        }
    }

    private fun setupListeners() {
        binding.btnAuthSubmit.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            setLoading(true)

            if (isSignUpMode) {
                val name = binding.etName.text.toString().trim()
                val location = binding.etLocation.text.toString().trim()
                val phone = binding.etPhone.text.toString().trim()

                if (name.isEmpty()) {
                    setLoading(false)
                    Toast.makeText(this, "Please enter your full name", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                UserManager.registerWithEmail(name, email, pass, location, phone) { success, error ->
                    setLoading(false)
                    if (success) {
                        Toast.makeText(this, "Welcome to BoiBinimoy, $name!", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    } else {
                        Toast.makeText(this, error ?: "Registration failed", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                UserManager.signInWithEmail(email, pass) { success, error ->
                    setLoading(false)
                    if (success) {
                        val userName = UserManager.currentUser?.name ?: "Reader"
                        Toast.makeText(this, "Welcome back, $userName!", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    } else {
                        Toast.makeText(this, error ?: "Sign in failed. Check your credentials.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.pbAuthLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnAuthSubmit.isEnabled = !isLoading
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
