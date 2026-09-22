package com.example.boibinimoy

import android.app.Application
import com.example.boibinimoy.data.FirestoreRepository
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        // Seed Firestore with mock data if collections are empty (first launch)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirestoreRepository.seedIfEmpty()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
