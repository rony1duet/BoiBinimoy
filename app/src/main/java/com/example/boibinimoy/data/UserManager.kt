package com.example.boibinimoy.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.example.boibinimoy.model.UserProfile
import com.example.boibinimoy.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest

object UserManager {

    private const val PREFS_NAME = "boibinimoy_user_prefs"
    private const val ACCOUNTS_PREFS_NAME = "boibinimoy_saved_accounts"
    private const val KEY_LOGGED_IN = "key_logged_in"
    private const val KEY_USER_ID = "key_user_id"
    private const val KEY_NAME = "key_name"
    private const val KEY_EMAIL = "key_email"
    private const val KEY_PHONE = "key_phone"
    private const val KEY_LOCATION = "key_location"
    private const val KEY_IS_ADMIN = "key_is_admin"
    private const val KEY_MEMBER_SINCE = "key_member_since"
    private const val KEY_BOOKS_LISTED = "key_books_listed"
    private const val KEY_BOOKS_SOLD = "key_books_sold"
    private const val KEY_BOOKS_EXCHANGED = "key_books_exchanged"
    private const val KEY_TOTAL_SAVINGS = "key_total_savings"

    private lateinit var prefs: SharedPreferences
    private lateinit var accountsPrefs: SharedPreferences
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
        accountsPrefs = context.getSharedPreferences(ACCOUNTS_PREFS_NAME, Context.MODE_PRIVATE)

        val isLoggedIn = prefs.getBoolean(KEY_LOGGED_IN, false)
        val firebaseUser = auth.currentUser

        if (firebaseUser != null || isLoggedIn) {
            val uid = firebaseUser?.uid ?: prefs.getString(KEY_USER_ID, "") ?: ""
            val name = firebaseUser?.displayName ?: prefs.getString(KEY_NAME, "") ?: ""
            val email = firebaseUser?.email ?: prefs.getString(KEY_EMAIL, "") ?: ""
            val phone = prefs.getString(KEY_PHONE, "") ?: ""
            val location = prefs.getString(KEY_LOCATION, "") ?: ""
            val isAdmin = prefs.getBoolean(KEY_IS_ADMIN, false)
            val memberSince = prefs.getString(KEY_MEMBER_SINCE, "") ?: ""
            val booksListed = prefs.getInt(KEY_BOOKS_LISTED, 0)
            val booksSold = prefs.getInt(KEY_BOOKS_SOLD, 0)
            val booksExchanged = prefs.getInt(KEY_BOOKS_EXCHANGED, 0)
            val totalSavings = prefs.getInt(KEY_TOTAL_SAVINGS, 0)

            val profile = UserProfile(
                id = uid,
                name = name,
                email = email,
                phone = phone,
                location = location,
                isAdmin = isAdmin,
                memberSince = memberSince,
                isVerified = true,
                booksListed = booksListed,
                booksSold = booksSold,
                booksExchanged = booksExchanged,
                totalSavingsTaka = totalSavings
            )
            currentUser = profile
            saveProfileLocally(profile)

            // Sync with Firestore asynchronously
            if (uid.isNotBlank() || email.isNotBlank()) {
                scope.launch {
                    try {
                        val isAdminFromDb = checkAdminStatusFromFirestore(email, uid)
                        var remoteDoc: DocumentSnapshot? = null
                        if (uid.isNotBlank()) {
                            val doc = db.collection("users").document(uid).get().await()
                            if (doc.exists()) remoteDoc = doc
                        }
                        val cleanEmail = email.trim().lowercase()
                        if (remoteDoc == null && cleanEmail.isNotBlank()) {
                            val emailDoc = db.collection("users").document(cleanEmail).get().await()
                            if (emailDoc.exists()) {
                                remoteDoc = emailDoc
                            } else {
                                val q = db.collection("users").whereEqualTo("email", cleanEmail).get().await()
                                if (!q.isEmpty) remoteDoc = q.documents[0]
                            }
                        }

                        val remoteProfile = if (remoteDoc != null && remoteDoc.exists()) {
                            documentToUserProfile(remoteDoc)
                        } else {
                            profile
                        }
                        val finalProfile = remoteProfile.copy(
                            isAdmin = remoteProfile.isAdmin || isAdminFromDb
                        )
                        currentUser = finalProfile
                        saveProfileLocally(finalProfile)

                        if (finalProfile.isAdmin && uid.isNotBlank()) {
                            try {
                                db.collection("users").document(uid).set(
                                    mapOf("isAdmin" to true, "admin" to true),
                                    SetOptions.merge()
                                ).await()
                            } catch (_: Exception) {}
                        }
                    } catch (_: Exception) {
                        // Offline fallback keeps current local profile
                    }
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

    /**
     * Checks if a document indicates admin privileges across any common field naming or structure.
     */
    fun isDocumentAdmin(doc: DocumentSnapshot): Boolean {
        val adminRaw = doc.get("admin")
            ?: doc.get("isAdmin")
            ?: doc.get("is_admin")
            ?: doc.get("Admin")
            ?: doc.get("IsAdmin")
            ?: doc.get("IS_ADMIN")
            ?: doc.get("role")
            ?: doc.get("Role")
            ?: doc.get("ROLE")
            ?: doc.get("userType")
            ?: doc.get("type")
            ?: doc.get("user_role")

        if (adminRaw != null) {
            when (adminRaw) {
                is Boolean -> if (adminRaw) return true
                is String -> {
                    val s = adminRaw.trim().lowercase()
                    if (s == "true" || s == "admin" || s == "1" || s == "yes") return true
                }
                is Number -> if (adminRaw.toInt() == 1) return true
            }
        }

        val roles = doc.get("roles")
        if (roles is Map<*, *>) {
            val rAdmin = roles["admin"] ?: roles["isAdmin"]
            if (rAdmin == true || rAdmin?.toString()?.trim()?.lowercase() in listOf("true", "admin", "1")) {
                return true
            }
        }

        if (doc.reference.path.startsWith("admins/") || doc.reference.path.startsWith("admin/")) {
            return true
        }

        return false
    }

    /**
     * Comprehensive Firestore check: checks users by uid, users by email doc ID,
     * queries where email == cleanEmail or original casing, dedicated admins collection,
     * and case-insensitive matching across users.
     */
    suspend fun checkAdminStatusFromFirestore(email: String, uid: String): Boolean {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank() && uid.isBlank()) return false

        // 1. Direct check on users collection by uid
        if (uid.isNotBlank()) {
            try {
                val doc = db.collection("users").document(uid).get().await()
                if (doc.exists() && isDocumentAdmin(doc)) return true
            } catch (_: Exception) {}
        }

        // 2. Direct check on users collection by cleanEmail doc id
        if (cleanEmail.isNotBlank()) {
            try {
                val doc = db.collection("users").document(cleanEmail).get().await()
                if (doc.exists() && isDocumentAdmin(doc)) return true
            } catch (_: Exception) {}
        }

        // 3. Query users collection where email == cleanEmail
        if (cleanEmail.isNotBlank()) {
            try {
                val query = db.collection("users").whereEqualTo("email", cleanEmail).get().await()
                for (doc in query.documents) {
                    if (isDocumentAdmin(doc)) return true
                }
            } catch (_: Exception) {}
        }

        // 4. Query users collection where email == email.trim() (case preservation fallback)
        if (email.trim().isNotBlank() && email.trim() != cleanEmail) {
            try {
                val query = db.collection("users").whereEqualTo("email", email.trim()).get().await()
                for (doc in query.documents) {
                    if (isDocumentAdmin(doc)) return true
                }
            } catch (_: Exception) {}
        }

        // 5. Check dedicated admins / admin collections
        val adminCollections = listOf("admins", "admin")
        for (col in adminCollections) {
            if (cleanEmail.isNotBlank()) {
                try {
                    val doc = db.collection(col).document(cleanEmail).get().await()
                    if (doc.exists() && (doc.data?.isEmpty() == false || isDocumentAdmin(doc))) return true
                } catch (_: Exception) {}
                try {
                    val query = db.collection(col).whereEqualTo("email", cleanEmail).get().await()
                    if (!query.isEmpty) return true
                } catch (_: Exception) {}
            }
            if (uid.isNotBlank()) {
                try {
                    val doc = db.collection(col).document(uid).get().await()
                    if (doc.exists() && (doc.data?.isEmpty() == false || isDocumentAdmin(doc))) return true
                } catch (_: Exception) {}
            }
        }

        // 6. Case-insensitive document scan across users in Firestore
        if (cleanEmail.isNotBlank()) {
            try {
                val allUsers = db.collection("users").get().await()
                for (doc in allUsers.documents) {
                    val docEmail = doc.getString("email")?.trim()?.lowercase()
                    if (docEmail == cleanEmail && isDocumentAdmin(doc)) {
                        return true
                    }
                }
            } catch (_: Exception) {}
        }

        return false
    }

    /**
     * Safely maps a Firestore document snapshot to UserProfile,
     * converting Longs to Double/Int gracefully and handling both 'admin' and 'isAdmin'.
     */
    fun documentToUserProfile(doc: DocumentSnapshot): UserProfile {
        val id = doc.getString("id") ?: doc.id
        val name = doc.getString("name") ?: ""
        val email = doc.getString("email") ?: ""
        val phone = doc.getString("phone") ?: ""
        val location = doc.getString("location") ?: ""
        val isVerified = doc.getBoolean("isVerified") ?: doc.getBoolean("verified") ?: false
        val memberSince = doc.getString("memberSince") ?: ""
        val rating = doc.getDouble("rating") ?: ((doc.getLong("rating") ?: 0L).toDouble())
        val reviewsCount = (doc.getLong("reviewsCount") ?: 0L).toInt()
        val booksListed = (doc.getLong("booksListed") ?: 0L).toInt()
        val booksSold = (doc.getLong("booksSold") ?: 0L).toInt()
        val booksExchanged = (doc.getLong("booksExchanged") ?: 0L).toInt()
        val totalSavingsTaka = (doc.getLong("totalSavingsTaka") ?: 0L).toInt()
        val isAdmin = isDocumentAdmin(doc)
        val passwordHash = doc.getString("passwordHash")
            ?: doc.getString("password")?.let { if (it.isNotBlank()) hashPassword(it) else "" }
            ?: ""

        return UserProfile(
            id = id,
            name = name,
            email = email,
            phone = phone,
            location = location,
            isVerified = isVerified,
            memberSince = memberSince,
            rating = rating,
            reviewsCount = reviewsCount,
            booksListed = booksListed,
            booksSold = booksSold,
            booksExchanged = booksExchanged,
            totalSavingsTaka = totalSavingsTaka,
            isAdmin = isAdmin,
            passwordHash = passwordHash
        )
    }

    /**
     * Maps a UserProfile into a Map for Firestore with dual keys (admin/isAdmin, verified/isVerified)
     * for full backwards-compatibility.
     */
    fun userProfileToMap(profile: UserProfile): Map<String, Any> {
        return mapOf(
            "id" to profile.id,
            "name" to profile.name,
            "email" to profile.email,
            "phone" to profile.phone,
            "location" to profile.location,
            "isVerified" to profile.isVerified,
            "verified" to profile.isVerified,
            "memberSince" to profile.memberSince,
            "rating" to profile.rating,
            "reviewsCount" to profile.reviewsCount,
            "booksListed" to profile.booksListed,
            "booksSold" to profile.booksSold,
            "booksExchanged" to profile.booksExchanged,
            "totalSavingsTaka" to profile.totalSavingsTaka,
            "isAdmin" to profile.isAdmin,
            "admin" to profile.isAdmin,
            "passwordHash" to profile.passwordHash
        )
    }

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun verifyPassword(inputPassword: String, storedHashOrPass: String): Boolean {
        if (storedHashOrPass.isBlank()) return true
        if (storedHashOrPass == inputPassword) return true
        return hashPassword(inputPassword).equals(storedHashOrPass, ignoreCase = true)
    }

    private fun saveRegisteredUserLocally(email: String, passwordHash: String, profile: UserProfile) {
        if (!::accountsPrefs.isInitialized) return
        val key = email.trim().lowercase()
        accountsPrefs.edit()
            .putString("${key}_id", profile.id)
            .putString("${key}_name", profile.name)
            .putString("${key}_email", profile.email)
            .putString("${key}_phone", profile.phone)
            .putString("${key}_location", profile.location)
            .putBoolean("${key}_is_admin", profile.isAdmin)
            .putString("${key}_member_since", profile.memberSince)
            .putString("${key}_password_hash", passwordHash)
            .apply()
    }

    private fun getLocalRegisteredUser(email: String): UserProfile? {
        if (!::accountsPrefs.isInitialized) return null
        val key = email.trim().lowercase()
        val id = accountsPrefs.getString("${key}_id", null) ?: return null
        val name = accountsPrefs.getString("${key}_name", "") ?: ""
        val userEmail = accountsPrefs.getString("${key}_email", email) ?: email
        val phone = accountsPrefs.getString("${key}_phone", "") ?: ""
        val location = accountsPrefs.getString("${key}_location", "") ?: ""
        val isAdmin = accountsPrefs.getBoolean("${key}_is_admin", false)
        val memberSince = accountsPrefs.getString("${key}_member_since", "") ?: ""
        val passHash = accountsPrefs.getString("${key}_password_hash", "") ?: ""

        return UserProfile(
            id = id,
            name = name,
            email = userEmail,
            phone = phone,
            location = location,
            isAdmin = isAdmin,
            memberSince = memberSince,
            isVerified = true,
            passwordHash = passHash
        )
    }

    fun signInWithEmail(
        email: String,
        pass: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val cleanEmail = email.trim().lowercase()

        scope.launch {
            // 1. Try Firebase Auth first
            var fbUid: String? = null
            try {
                val authResult = auth.signInWithEmailAndPassword(cleanEmail, pass).await()
                fbUid = authResult.user?.uid
            } catch (e: Exception) {
                android.util.Log.w("UserManager", "FirebaseAuth signIn skipped/failed: ${e.message}")
            }

            if (fbUid != null) {
                try {
                    var doc = db.collection("users").document(fbUid).get().await()
                    if (!doc.exists()) {
                        val emailDoc = db.collection("users").document(cleanEmail).get().await()
                        if (emailDoc.exists()) {
                            doc = emailDoc
                        } else {
                            val q = db.collection("users").whereEqualTo("email", cleanEmail).get().await()
                            if (!q.isEmpty) {
                                doc = q.documents[0]
                            }
                        }
                    }

                    // Check admin status across Firestore
                    val isAdminFromDb = checkAdminStatusFromFirestore(cleanEmail, fbUid)

                    val profile = if (doc.exists()) {
                        // Purge legacy plain password field if present in Firestore
                        if (doc.contains("password")) {
                            try {
                                val updates = mutableMapOf<String, Any>("password" to FieldValue.delete())
                                if (!doc.contains("passwordHash") || doc.getString("passwordHash").isNullOrBlank()) {
                                    updates["passwordHash"] = hashPassword(pass)
                                }
                                db.collection("users").document(doc.id).update(updates)
                            } catch (_: Exception) {}
                        }
                        val p = documentToUserProfile(doc)
                        p.copy(isAdmin = p.isAdmin || isAdminFromDb)
                    } else {
                        val fbUser = auth.currentUser
                        val p = UserProfile(
                            id = fbUid,
                            name = fbUser?.displayName ?: cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() },
                            email = cleanEmail,
                            location = "",
                            isAdmin = isAdminFromDb,
                            memberSince = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.ENGLISH).format(java.util.Date()),
                            isVerified = false,
                            passwordHash = hashPassword(pass)
                        )
                        db.collection("users").document(fbUid).set(userProfileToMap(p)).await()
                        p
                    }
                    currentUser = profile
                    saveProfileLocally(profile)
                    saveRegisteredUserLocally(cleanEmail, profile.passwordHash.ifBlank { hashPassword(pass) }, profile)

                    if (profile.isAdmin) {
                        try {
                            db.collection("users").document(fbUid).set(
                                mapOf("isAdmin" to true, "admin" to true),
                                SetOptions.merge()
                            ).await()
                        } catch (_: Exception) {}
                    }

                    withContext(Dispatchers.Main) {
                        onResult(true, null)
                    }
                    return@launch
                } catch (e: Exception) {
                    android.util.Log.w("UserManager", "FirebaseAuth user lookup failed: ${e.message}")
                    // Fall through to Firestore database lookup
                }
            }

            // 2. Query Firestore Database directly
            try {
                var querySnapshot = db.collection("users")
                    .whereEqualTo("email", cleanEmail)
                    .get().await()

                if (querySnapshot.isEmpty && cleanEmail != email.trim()) {
                    querySnapshot = db.collection("users")
                        .whereEqualTo("email", email.trim())
                        .get().await()
                }

                var foundDoc: DocumentSnapshot? = if (!querySnapshot.isEmpty) querySnapshot.documents[0] else null
                if (foundDoc == null) {
                    val directDoc = db.collection("users").document(cleanEmail).get().await()
                    if (directDoc.exists()) foundDoc = directDoc
                }

                if (foundDoc != null) {
                    val doc = foundDoc
                    val profile = documentToUserProfile(doc)
                    val storedPass = doc.getString("passwordHash") ?: doc.getString("password") ?: profile.passwordHash

                    if (verifyPassword(pass, storedPass)) {
                        val isAdminFromDb = checkAdminStatusFromFirestore(cleanEmail, doc.id) || profile.isAdmin
                        val finalProfile = profile.copy(isAdmin = isAdminFromDb)

                        // Purge legacy plain password field if present in Firestore
                        if (doc.contains("password")) {
                            try {
                                val updates = mutableMapOf<String, Any>("password" to FieldValue.delete())
                                if (!doc.contains("passwordHash") || doc.getString("passwordHash").isNullOrBlank()) {
                                    updates["passwordHash"] = hashPassword(pass)
                                }
                                db.collection("users").document(doc.id).update(updates)
                            } catch (_: Exception) {}
                        }

                        currentUser = finalProfile
                        saveProfileLocally(finalProfile)
                        saveRegisteredUserLocally(cleanEmail, finalProfile.passwordHash.ifBlank { hashPassword(pass) }, finalProfile)

                        if (finalProfile.isAdmin) {
                            try {
                                db.collection("users").document(doc.id).set(
                                    mapOf("isAdmin" to true, "admin" to true),
                                    SetOptions.merge()
                                ).await()
                            } catch (_: Exception) {}
                        }

                        withContext(Dispatchers.Main) {
                            onResult(true, null)
                        }
                        return@launch
                    } else {
                        withContext(Dispatchers.Main) {
                            onResult(false, "Incorrect password. Please try again.")
                        }
                        return@launch
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("UserManager", "Firestore signIn query failed: ${e.message}")
            }

            // 3. Check local registered user cache (offline support)
            val cachedUser = getLocalRegisteredUser(cleanEmail)
            if (cachedUser != null) {
                if (verifyPassword(pass, cachedUser.passwordHash)) {
                    currentUser = cachedUser
                    saveProfileLocally(cachedUser)
                    withContext(Dispatchers.Main) {
                        onResult(true, null)
                    }
                    return@launch
                } else {
                    withContext(Dispatchers.Main) {
                        onResult(false, "Incorrect password. Please try again.")
                    }
                    return@launch
                }
            }

            // 4. No user found
            withContext(Dispatchers.Main) {
                onResult(false, "No account found with this email. Please create an account first.")
            }
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
        val cleanEmail = email.trim().lowercase()
        val passHash = hashPassword(pass)
        val isAdminUser = false

        scope.launch {
            // 1. Check if email is already taken in Firestore database
            try {
                val existing = db.collection("users")
                    .whereEqualTo("email", cleanEmail)
                    .get().await()
                if (!existing.isEmpty) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "An account with this email already exists. Please sign in.")
                    }
                    return@launch
                }
            } catch (e: Exception) {
                val cached = getLocalRegisteredUser(cleanEmail)
                if (cached != null) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "An account with this email already exists locally. Please sign in.")
                    }
                    return@launch
                }
            }

            // 2. Try creating in FirebaseAuth if enabled
            var uid = ""
            try {
                val authResult = auth.createUserWithEmailAndPassword(cleanEmail, pass).await()
                val fbUser = authResult.user
                uid = fbUser?.uid ?: ""
                fbUser?.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(name).build()
                )?.await()
            } catch (e: Exception) {
                android.util.Log.w("UserManager", "FirebaseAuth createUser skipped/failed: ${e.message}")
            }

            if (uid.isBlank()) {
                uid = "user_${System.currentTimeMillis()}"
            }

            val profile = UserProfile(
                id = uid,
                name = name,
                email = cleanEmail,
                phone = phone.trim(),
                location = location.trim(),
                isAdmin = false,
                memberSince = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.ENGLISH).format(java.util.Date()),
                isVerified = false,
                passwordHash = passHash
            )

            // 3. Save to Firestore database (strictly passwordHash, never plain password)
            try {
                val map = userProfileToMap(profile)
                db.collection("users").document(uid).set(map).await()
            } catch (e: Exception) {
                android.util.Log.w("UserManager", "Firestore set user failed: ${e.message}")
            }

            // 4. Save locally
            currentUser = profile
            saveProfileLocally(profile)
            saveRegisteredUserLocally(cleanEmail, passHash, profile)

            withContext(Dispatchers.Main) {
                onResult(true, null)
            }
        }
    }

    fun refreshCurrentUser(onComplete: ((UserProfile?) -> Unit)? = null) {
        val user = currentUser
        val uid = user?.id ?: auth.currentUser?.uid ?: ""
        val email = user?.email ?: auth.currentUser?.email ?: (if (::prefs.isInitialized) prefs.getString(KEY_EMAIL, "") ?: "" else "")
        if (uid.isBlank() && email.isBlank()) {
            onComplete?.invoke(currentUser)
            return
        }

        scope.launch {
            try {
                // Find primary document
                var foundDoc: DocumentSnapshot? = null
                if (uid.isNotBlank()) {
                    val doc = db.collection("users").document(uid).get().await()
                    if (doc.exists()) foundDoc = doc
                }
                val cleanEmail = email.trim().lowercase()
                if (foundDoc == null && cleanEmail.isNotBlank()) {
                    val docByEmail = db.collection("users").document(cleanEmail).get().await()
                    if (docByEmail.exists()) {
                        foundDoc = docByEmail
                    } else {
                        val q = db.collection("users").whereEqualTo("email", cleanEmail).get().await()
                        if (!q.isEmpty) foundDoc = q.documents[0]
                    }
                }

                // Check admin status across all Firestore possibilities
                val isAdminFromDb = checkAdminStatusFromFirestore(email, uid)

                val baseProfile = if (foundDoc != null && foundDoc.exists()) {
                    documentToUserProfile(foundDoc)
                } else {
                    user ?: UserProfile(id = uid, email = email)
                }

                val finalProfile = baseProfile.copy(
                    isAdmin = baseProfile.isAdmin || isAdminFromDb
                )

                currentUser = finalProfile
                saveProfileLocally(finalProfile)

                // If user is admin from DB, ensure their primary document in users collection has isAdmin = true
                if (finalProfile.isAdmin && uid.isNotBlank()) {
                    try {
                        db.collection("users").document(uid).set(
                            mapOf("isAdmin" to true, "admin" to true),
                            SetOptions.merge()
                        ).await()
                    } catch (_: Exception) {}
                }

                withContext(Dispatchers.Main) {
                    onComplete?.invoke(finalProfile)
                }
            } catch (e: Exception) {
                android.util.Log.w("UserManager", "refreshCurrentUser failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(currentUser)
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

    fun incrementUserBooksListed() {
        val existing = currentUser ?: return
        val updated = existing.copy(booksListed = existing.booksListed + 1)
        currentUser = updated
        saveProfileLocally(updated)
        scope.launch {
            try {
                FirestoreRepository.updateUserProfile(updated)
            } catch (_: Exception) {}
        }
    }

    fun saveProfileLocally(profile: UserProfile) {
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
            .putInt(KEY_BOOKS_LISTED, profile.booksListed)
            .putInt(KEY_BOOKS_SOLD, profile.booksSold)
            .putInt(KEY_BOOKS_EXCHANGED, profile.booksExchanged)
            .putInt(KEY_TOTAL_SAVINGS, profile.totalSavingsTaka)
            .apply()
    }
}
