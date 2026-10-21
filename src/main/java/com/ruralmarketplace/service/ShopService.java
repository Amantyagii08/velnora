

package com.ruralmarketplace.service;

import com.ruralmarketplace.dto.CreateShopRequest;
import com.ruralmarketplace.dto.UpdateShopRequest;
import com.ruralmarketplace.entity.Category;
import com.ruralmarketplace.entity.Shop;
import com.ruralmarketplace.exception.CategoryNotFoundException;
import com.ruralmarketplace.exception.ShopNotFoundException;
import com.ruralmarketplace.repository.CategoryRepository;
import com.ruralmarketplace.repository.ShopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.ruralmarketplace.service.image.ImageStorageService;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Service
public class ShopService {

    private final ShopRepository shopRepository;
    private final CategoryRepository categoryRepository;
    private final ImageStorageService imageStorageService;

    public ShopService(
            ShopRepository shopRepository,
            CategoryRepository categoryRepository,
            ImageStorageService imageStorageService) {
        this.shopRepository = shopRepository;
        this.categoryRepository = categoryRepository;
        this.imageStorageService = imageStorageService;
    }

    
    @Transactional
    public Shop createShop(CreateShopRequest request,
                            MultipartFile image
    )throws IOException {

        Category category = categoryRepository
                .findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Category not found with ID: "
                                + request.getCategoryId()
                ));

        Shop shop = new Shop();
        shop.setName(request.getName().trim());
        shop.setDescription(request.getDescription());
        shop.setAddress(request.getAddress().trim());
        shop.setCategory(category);

        if(image != null && !image.isEmpty()){
            String imagePath = imageStorageService.storeImage(image,"shops");

            shop.setImagePath(imagePath);
        }

        return shopRepository.save(shop);
    }


    @Transactional
public Shop updateShop(
        Long shopId,
        UpdateShopRequest request,
        MultipartFile image) throws IOException {

    Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() ->
                    new ShopNotFoundException(
                            "Shop not found with ID: " + shopId));

    Category category = categoryRepository.findById(
            request.getCategoryId())
            .orElseThrow(() ->
                    new CategoryNotFoundException(
                            "Category not found with ID: "
                                    + request.getCategoryId()));

    shop.setName(request.getName().trim());
    shop.setDescription(request.getDescription());
    shop.setAddress(request.getAddress().trim());
    shop.setCategory(category);

    if (image != null && !image.isEmpty()) {
        String imagePath =
                imageStorageService.storeImage(image, "shops");
        shop.setImagePath(imagePath);
    }

    return shopRepository.save(shop);
}

    

    @Transactional(readOnly = true)
    public List<Shop> getAllShops() {
        return shopRepository.findAll();
    }

    
    @Transactional(readOnly = true)
    public Shop getShopById(Long shopId) {

        return shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFoundException(
                        "Shop not found with ID: " + shopId
                ));
    }

    
    @Transactional(readOnly = true)
    public List<Shop> searchShops(String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return getAllShops();
        }

        return shopRepository.findByNameContainingIgnoreCase(
                keyword.trim()
        );
    }

    
    @Transactional(readOnly = true)
    public List<Shop> getShopsByCategory(Long categoryId) {

        if (!categoryRepository.existsById(categoryId)) {
            throw new CategoryNotFoundException(
                    "Category not found with ID: " + categoryId
            );
        }

        return shopRepository.findByCategory_Id(categoryId);
    }
}


