
package com.ruralmarketplace.controller;

import com.ruralmarketplace.entity.Category;
import com.ruralmarketplace.service.CategoryService;


import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.data.domain.Page;
import com.ruralmarketplace.dto.CategoryResponse;


@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public Page<CategoryResponse> getAllCategories(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return categoryService.getActiveCategories(page, size).map(CategoryResponse::from);
    }
}

