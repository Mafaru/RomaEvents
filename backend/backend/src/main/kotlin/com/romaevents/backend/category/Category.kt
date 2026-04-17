package com.romaevents.backend.category

import jakarta.persistence.*

@Entity
@Table(name = "categories")
data class Category(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val name: String,

    val description: String?
)