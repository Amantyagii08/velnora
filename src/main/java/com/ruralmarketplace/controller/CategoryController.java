
package com.ruralmarketplace.controller;

import com.ruralmarketplace.entity.Category;
import com.ruralmarketplace.service.CategoryService;


import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

   @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
   @PreAuthorize("hasRole('ADMIN')")
public Category createCategory(
        @ModelAttribute Category category,
        @RequestParam(value = "image", required = false)
        MultipartFile image) throws IOException {

    return categoryService.createCategory(category, image);
}

    @GetMapping
    public List<Category> getAllCategories() {
        return categoryService.getAllCategories();
    }
}

