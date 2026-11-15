
package com.ruralmarketplace.service;

import com.ruralmarketplace.entity.Category;
import com.ruralmarketplace.dto.CategoryRequest;
import com.ruralmarketplace.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruralmarketplace.service.image.ImageStorageService;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

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

    @Transactional
    public Category createCategory(CategoryRequest request, MultipartFile image)throws IOException {
        Category category = new Category();
        category.setName(request.getName().trim());
        if(image != null && !image.isEmpty()){
            String imagePath = imageStorageService.storeImage(image,"categories");

            category.setImagePath(imagePath);
        }
        return categoryRepository.save(category);
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Page<Category> getActiveCategories(int page, int size) {
        return categoryRepository.findByActiveTrue(
                PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 100)));
    }

    public String storeCategoryImage(MultipartFile image) throws IOException {
        return imageStorageService.storeImage(image, "categories");
    }

    public void deleteStoredImage(String imagePath) throws IOException {
        imageStorageService.deleteImageAfterCommit(imagePath);
    }
}

