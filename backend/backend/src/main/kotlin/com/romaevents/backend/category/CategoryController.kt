package com.romaevents.backend.category

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/categories")
class CategoryController(
    private val categoryRepository: CategoryRepository
) {

    @GetMapping
    fun getAll(): List<Category> {
        return categoryRepository.findAll()
    }
}