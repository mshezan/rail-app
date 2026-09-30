package com.example.railapp.auth

enum class UserRole {
    ADMIN, PASSENGER, GUEST
}

data class User(
    val id: String,
    val email: String,
    val role: UserRole
)
