
package com.ruralmarketplace.service;

import com.ruralmarketplace.entity.Category;
import com.ruralmarketplace.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import com.ruralmarketplace.service.image.ImageStorageService;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ImageStorageService imageStorageService;

    public CategoryService(CategoryRepository categoryRepository,
                            ImageStorageService imageStorageService
    ) {
        this.categoryRepository = categoryRepository;
        this.imageStorageService = imageStorageService;
    }

    public Category createCategory(Category category, MultipartFile image)throws IOException {
        if(image != null && !image.isEmpty()){
            String imagePath = imageStorageService.storeImage(image,"categories");

            category.setImagePath(imagePath);
        }
        return categoryRepository.save(category);
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
}

