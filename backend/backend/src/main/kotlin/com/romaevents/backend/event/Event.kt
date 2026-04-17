package com.romaevents.backend.event

import com.romaevents.backend.category.Category
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "events")
class Event(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val title: String,

    val description: String? = null,

    val rawDateText: String? = null,

    val address: String? = null,

    val latitude: Double? = null,
    val longitude: Double? = null,

    @ManyToOne
    @JoinColumn(name = "category_id")
    val category: Category? = null,

    val sourceUrl: String? = null,

    val createdAt: LocalDateTime? = null
)