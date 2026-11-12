

package com.ruralmarketplace.service;

import com.ruralmarketplace.dto.CreateShopRequest;
import com.ruralmarketplace.dto.UpdateShopRequest;
import com.ruralmarketplace.entity.Category;
import com.ruralmarketplace.entity.Shop;
import com.ruralmarketplace.entity.ShopApprovalStatus;
import com.ruralmarketplace.entity.User;
import com.ruralmarketplace.entity.UserRole;
import com.ruralmarketplace.exception.CategoryNotFoundException;
import com.ruralmarketplace.exception.ShopNotFoundException;
import com.ruralmarketplace.repository.CategoryRepository;
import com.ruralmarketplace.repository.ShopRepository;
import com.ruralmarketplace.repository.UserRepository;
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
    private final UserRepository userRepository;

    public void requireShopOwner(Long shopId, String email) {
        Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new ShopNotFoundException("Shop not found with ID: " + shopId));
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!shop.getOwner().getId().equals(user.getId())) throw new IllegalArgumentException("You can only manage your own shop");
    }

    @Transactional(readOnly = true)
    public Shop getMyShop(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRole() != UserRole.SHOPKEEPER) throw new IllegalArgumentException("Only shopkeepers have shops");
        return shopRepository.findByOwner_Id(user.getId()).orElseThrow(() -> new IllegalArgumentException("No shop application found"));
    }

    public ShopService(
            ShopRepository shopRepository,
            CategoryRepository categoryRepository,
            ImageStorageService imageStorageService,
            UserRepository userRepository) {
        this.shopRepository = shopRepository;
        this.categoryRepository = categoryRepository;
        this.imageStorageService = imageStorageService;
        this.userRepository = userRepository;
    }

    
    @Transactional
    public Shop createShop(CreateShopRequest request,
                            MultipartFile image, String ownerEmail
    )throws IOException {

        Category category = categoryRepository
                .findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Category not found with ID: "
                                + request.getCategoryId()
                ));

        Shop shop = new Shop();
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Shopkeeper account not found"));
        if (owner.getRole() != UserRole.SHOPKEEPER) {
            throw new IllegalArgumentException("Only shopkeepers can submit a shop");
        }
        if (shopRepository.findByOwner_Id(owner.getId()).isPresent()) {
            throw new IllegalArgumentException("This shopkeeper already has a shop application");
        }
        shop.setName(request.getName().trim());
        shop.setDescription(request.getDescription());
        shop.setAddress(request.getAddress().trim());
        shop.setCategory(category);
        shop.setOwner(owner);
        shop.setApprovalStatus(ShopApprovalStatus.PENDING);

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
        MultipartFile image, String ownerEmail) throws IOException {

    requireShopOwner(shopId, ownerEmail);

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
        return shopRepository.findByApprovalStatusAndActiveTrue(ShopApprovalStatus.APPROVED);
    }

    
    @Transactional(readOnly = true)
    public Shop getShopById(Long shopId) {

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFoundException(
                        "Shop not found with ID: " + shopId
                ));
        if (shop.getApprovalStatus() != ShopApprovalStatus.APPROVED || !shop.isActive()) {
            throw new ShopNotFoundException("Shop not found with ID: " + shopId);
        }
        return shop;
    }

    
    @Transactional(readOnly = true)
    public List<Shop> searchShops(String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return getAllShops();
        }

        return shopRepository.findByNameContainingIgnoreCase(keyword.trim()).stream()
                .filter(s -> s.getApprovalStatus() == ShopApprovalStatus.APPROVED && s.isActive()).toList();
    }

    
    @Transactional(readOnly = true)
    public List<Shop> getShopsByCategory(Long categoryId) {

        if (!categoryRepository.existsById(categoryId)) {
            throw new CategoryNotFoundException(
                    "Category not found with ID: " + categoryId
            );
        }

        return shopRepository.findByCategory_Id(categoryId).stream()
                .filter(s -> s.getApprovalStatus() == ShopApprovalStatus.APPROVED && s.isActive()).toList();
    }
}


