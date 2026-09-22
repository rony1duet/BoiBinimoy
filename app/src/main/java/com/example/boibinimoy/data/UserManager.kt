package com.example.boibinimoy.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.example.boibinimoy.model.UserProfile
import com.example.boibinimoy.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object UserManager {

    private const val PREFS_NAME = "boibinimoy_user_prefs"
    private const val KEY_LOGGED_IN = "key_logged_in"
    private const val KEY_USER_ID = "key_user_id"
    private const val KEY_NAME = "key_name"
    private const val KEY_EMAIL = "key_email"
    private const val KEY_PHONE = "key_phone"
    private const val KEY_LOCATION = "key_location"
    private const val KEY_IS_ADMIN = "key_is_admin"
    private const val KEY_MEMBER_SINCE = "key_member_since"

    private lateinit var prefs: SharedPreferences
    private val scope = CoroutineScope(Dispatchers.IO)
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _currentUserFlow = MutableStateFlow<UserProfile?>(null)
    val currentUserFlow: StateFlow<UserProfile?> = _currentUserFlow.asStateFlow()

    var currentUser: UserProfile?
        get() = _currentUserFlow.value
        private set(value) {
            _currentUserFlow.value = value
        }

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val isLoggedIn = prefs.getBoolean(KEY_LOGGED_IN, false)
        val firebaseUser = auth.currentUser

        if (firebaseUser != null || isLoggedIn) {
            val uid = firebaseUser?.uid ?: prefs.getString(KEY_USER_ID, "user_default") ?: "user_default"
            val name = firebaseUser?.displayName ?: prefs.getString(KEY_NAME, "Riad Hasan") ?: "Riad Hasan"
            val email = firebaseUser?.email ?: prefs.getString(KEY_EMAIL, "riad.hasan@example.com") ?: "riad.hasan@example.com"
            val phone = prefs.getString(KEY_PHONE, "+880 1712-345678") ?: "+880 1712-345678"
            val location = prefs.getString(KEY_LOCATION, "Dhanmondi, Dhaka") ?: "Dhanmondi, Dhaka"
            val isAdmin = prefs.getBoolean(KEY_IS_ADMIN, false)
            val memberSince = prefs.getString(KEY_MEMBER_SINCE, "January 2024") ?: "January 2024"

            val profile = UserProfile(
                id = uid,
                name = name,
                email = email,
                phone = phone,
                location = location,
                isAdmin = isAdmin,
                memberSince = memberSince,
                isVerified = true
            )
            currentUser = profile
            saveProfileLocally(profile)

            // Sync with Firestore asynchronously
            scope.launch {
                try {
                    val doc = db.collection("users").document(uid).get().await()
                    if (doc.exists()) {
                        val remoteProfile = doc.toObject(UserProfile::class.java)
                        if (remoteProfile != null) {
                            currentUser = remoteProfile
                            saveProfileLocally(remoteProfile)
                        }
                    } else {
                        db.collection("users").document(uid).set(profile).await()
                    }
                } catch (_: Exception) {
                    // Offline fallback keeps current local profile
                }
            }
        }
    }

    fun isLoggedIn(): Boolean {
        return currentUser != null || auth.currentUser != null || (::prefs.isInitialized && prefs.getBoolean(KEY_LOGGED_IN, false))
    }

    fun isAdmin(): Boolean {
        return currentUser?.isAdmin ?: (::prefs.isInitialized && prefs.getBoolean(KEY_IS_ADMIN, false))
    }

    fun signInWithEmail(
        email: String,
        pass: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val fbUser = result.user
                val uid = fbUser?.uid ?: "user_${System.currentTimeMillis()}"
                val name = fbUser?.displayName ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }

                val profile = UserProfile(
                    id = uid,
                    name = name,
                    email = email,
                    location = "Dhaka, Bangladesh",
                    isAdmin = email.contains("admin", ignoreCase = true),
                    memberSince = "Recently"
                )

                scope.launch {
                    try {
                        val doc = db.collection("users").document(uid).get().await()
                        val finalProfile = if (doc.exists()) {
                            doc.toObject(UserProfile::class.java) ?: profile
                        } else {
                            db.collection("users").document(uid).set(profile).await()
                            profile
                        }
                        currentUser = finalProfile
                        saveProfileLocally(finalProfile)
                        Dispatchers.Main.let {
                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                onResult(true, null)
                            }
                        }
                    } catch (e: Exception) {
                        currentUser = profile
                        saveProfileLocally(profile)
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            onResult(true, null)
                        }
                    }
                }
            }
            .addOnFailureListener { error ->
                onResult(false, error.localizedMessage ?: "Authentication failed")
            }
    }

    fun registerWithEmail(
        name: String,
        email: String,
        pass: String,
        location: String,
        phone: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val fbUser = result.user
                val uid = fbUser?.uid ?: "user_${System.currentTimeMillis()}"

                fbUser?.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(name).build()
                )

                val profile = UserProfile(
                    id = uid,
                    name = name,
                    email = email,
                    phone = phone.ifBlank { "+880 1712-000000" },
                    location = location.ifBlank { "Dhaka, Bangladesh" },
                    isAdmin = email.contains("admin", ignoreCase = true),
                    memberSince = "Today",
                    isVerified = true
                )

                scope.launch {
                    try {
                        db.collection("users").document(uid).set(profile).await()
                    } catch (_: Exception) {}
                    currentUser = profile
                    saveProfileLocally(profile)
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        onResult(true, null)
                    }
                }
            }
            .addOnFailureListener { error ->
                onResult(false, error.localizedMessage ?: "Registration failed")
            }
    }

    fun signInAsDemoUser() {
        val profile = UserProfile(
            id = "user_demo_regular",
            name = "Riad Hasan",
            email = "riad.hasan@example.com",
            phone = "+880 1712-345678",
            location = "Dhanmondi, Dhaka",
            isVerified = true,
            memberSince = "January 2024",
            booksListed = 12,
            booksSold = 28,
            booksExchanged = 15,
            isAdmin = false
        )
        currentUser = profile
        saveProfileLocally(profile)

        scope.launch {
            try {
                db.collection("users").document(profile.id).set(profile).await()
            } catch (_: Exception) {}
        }
    }

    fun signInAsDemoAdmin() {
        val profile = UserProfile(
            id = "user_demo_admin",
            name = "Admin Riad",
            email = "admin@boibinimoy.com",
            phone = "+880 1800-000000",
            location = "Dhaka Central HQ",
            isVerified = true,
            memberSince = "Founder",
            booksListed = 45,
            booksSold = 98,
            booksExchanged = 40,
            isAdmin = true
        )
        currentUser = profile
        saveProfileLocally(profile)

        scope.launch {
            try {
                db.collection("users").document(profile.id).set(profile).await()
            } catch (_: Exception) {}
        }
    }

    fun setAdminMode(isAdmin: Boolean, onComplete: ((Boolean) -> Unit)? = null) {
        val existing = currentUser ?: UserProfile()
        val updated = existing.copy(isAdmin = isAdmin)
        currentUser = updated
        saveProfileLocally(updated)

        scope.launch {
            try {
                FirestoreRepository.toggleAdminRole(updated.id, isAdmin)
                FirestoreRepository.updateUserProfile(updated)
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onComplete?.invoke(true)
                }
            } catch (_: Exception) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onComplete?.invoke(true)
                }
            }
        }
    }

    fun signOut(context: Context) {
        try {
            auth.signOut()
        } catch (_: Exception) {}

        if (::prefs.isInitialized) {
            prefs.edit().clear().apply()
        }
        currentUser = null

        val intent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }

    private fun saveProfileLocally(profile: UserProfile) {
        if (!::prefs.isInitialized) return
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_USER_ID, profile.id)
            .putString(KEY_NAME, profile.name)
            .putString(KEY_EMAIL, profile.email)
            .putString(KEY_PHONE, profile.phone)
            .putString(KEY_LOCATION, profile.location)
            .putBoolean(KEY_IS_ADMIN, profile.isAdmin)
            .putString(KEY_MEMBER_SINCE, profile.memberSince)
            .apply()
    }
}
