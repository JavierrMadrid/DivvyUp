package com.example.divvyup.domain.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val groups: List<Group> = emptyList()
)