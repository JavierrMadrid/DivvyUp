package com.example.divvyup.domain.model

data class Group(
    val id: String = "",
    val name: String = "",
    val users: List<User> = emptyList(),
    val spends: List<Spend> = emptyList()
)