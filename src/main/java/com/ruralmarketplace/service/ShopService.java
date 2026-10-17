

package com.ruralmarketplace.service;

import com.ruralmarketplace.dto.CreateShopRequest;
import com.ruralmarketplace.entity.Category;
import com.ruralmarketplace.entity.Shop;
import com.ruralmarketplace.exception.CategoryNotFoundException;
import com.ruralmarketplace.exception.ShopNotFoundException;
import com.ruralmarketplace.repository.CategoryRepository;
import com.ruralmarketplace.repository.ShopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ShopService {

    private final ShopRepository shopRepository;
    private final CategoryRepository categoryRepository;

    public ShopService(
            ShopRepository shopRepository,
            CategoryRepository categoryRepository) {
        this.shopRepository = shopRepository;
        this.categoryRepository = categoryRepository;
    }

    
    @Transactional
    public Shop createShop(CreateShopRequest request) {

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

        return shopRepository.save(shop);
    }

    
    @Transactional(readOnly = true)
    public List<Shop> getAllShops() {
        return shopRepository.findAll();
    }

    
    @Transactional(readOnly = true)
    public Shop getShopById(Long id) {

        return shopRepository.findById(id)
                .orElseThrow(() -> new ShopNotFoundException(
                        "Shop not found with ID: " + id
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


