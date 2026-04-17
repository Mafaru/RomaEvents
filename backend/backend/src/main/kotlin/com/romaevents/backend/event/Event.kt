package com.romaevents.backend.event

import com.romaevents.backend.category.Category
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "events")
data class Event(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val title: String,

    val description: String?,

    val rawDateText: String?,

    val address: String?,

    val latitude: Double?,
    val longitude: Double?,

    @ManyToOne
    @JoinColumn(name = "category_id")
    val category: Category?,

    val sourceUrl: String?,

    val createdAt: LocalDateTime? = null
)