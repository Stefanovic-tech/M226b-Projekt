package com.example

data class User(val id: Int, val username: String, val password: String, val role: Role)

enum class Role {
    USER, BOSS
}