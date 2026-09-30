package com.example.railapp

import android.app.Application
import com.example.railapp.di.AppContainer

class RailAppApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
