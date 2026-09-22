package com.example.boibinimoy

import android.app.Application
import com.example.boibinimoy.data.UserManager
import com.google.firebase.FirebaseApp

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        UserManager.init(this)
    }
}
