package com.example.railapp.di

import android.content.Context
import com.example.railapp.BuildConfig
import com.example.railapp.auth.SessionManager
import com.example.railapp.data.AppDatabase
import com.example.railapp.data.ScanRepository
import com.example.railapp.data.train.TrainStatusRepository
import com.example.railapp.sensor.DetectionSensor
import com.example.railapp.sensor.SimulatedSensor
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

class AppContainer(private val context: Context) {
    val supabaseClient: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            install(Realtime)
        }
    }
    val database: AppDatabase by lazy { AppDatabase.getDatabase(context) }
    val scanRepository: ScanRepository by lazy { ScanRepository(database.scanEventDao()) }
    val detectionSensor: DetectionSensor by lazy { SimulatedSensor() }
    val trainStatusRepository: TrainStatusRepository by lazy { TrainStatusRepository() }
    val sessionManager: SessionManager by lazy { SessionManager(supabaseClient) }
}
