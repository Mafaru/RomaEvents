package com.romaevents.backend.event

import org.springframework.data.jpa.repository.JpaRepository

interface EventRepository : JpaRepository<Event, Long> {
    fun findByCategoryId(categoryId: Long): List<Event>
}