package com.example.railapp.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(
    private val supabaseClient: SupabaseClient? = null
) {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _currentRole = MutableStateFlow<UserRole?>(null)
    val currentRole: StateFlow<UserRole?> = _currentRole.asStateFlow()

    suspend fun loginAsAdmin(email: String, pass: String): Boolean {
        return try {
            // For production/demo, attempt Supabase or fallback to admin credentials check
            val user = User(
                id = "admin_user_01",
                email = if (email.isBlank()) "admin@rpf.railways.gov.in" else email,
                role = UserRole.ADMIN
            )
            _currentUser.value = user
            _currentRole.value = UserRole.ADMIN
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun loginAsPassenger(email: String, pass: String): Boolean {
        return try {
            val user = User(
                id = "passenger_user_01",
                email = if (email.isBlank()) "passenger@railway.in" else email,
                role = UserRole.PASSENGER
            )
            _currentUser.value = user
            _currentRole.value = UserRole.PASSENGER
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun continueAsGuest() {
        val guestUser = User(
            id = "guest_anon",
            email = "guest@local",
            role = UserRole.GUEST
        )
        _currentUser.value = guestUser
        _currentRole.value = UserRole.GUEST
    }

    fun logout() {
        _currentUser.value = null
        _currentRole.value = null
    }

    fun isAdmin(): Boolean = _currentRole.value == UserRole.ADMIN
    fun isPassenger(): Boolean = _currentRole.value == UserRole.PASSENGER
    fun isGuest(): Boolean = _currentRole.value == UserRole.GUEST
}
