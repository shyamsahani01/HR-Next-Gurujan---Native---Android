package com.example.hrnext

import android.app.Application
import com.example.hrnext.di.AppContainer

class HRNextApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
