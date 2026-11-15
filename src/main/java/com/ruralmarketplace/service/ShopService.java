

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
import com.ruralmarketplace.exception.ResourceNotFoundException;
import com.ruralmarketplace.repository.CategoryRepository;
import com.ruralmarketplace.repository.ShopRepository;
import com.ruralmarketplace.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.ruralmarketplace.service.image.ImageStorageService;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.ruralmarketplace.util.SearchText;

@Service
public class ShopService {

    private final ShopRepository shopRepository;
    private final CategoryRepository categoryRepository;
    private final ImageStorageService imageStorageService;
    private final UserRepository userRepository;

    public void requireShopOwner(Long shopId, String email) {
        Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new ShopNotFoundException("Shop not found with ID: " + shopId));
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (shop.getOwner() == null || !shop.getOwner().getId().equals(user.getId())) throw new AccessDeniedException("You can only manage your own shop");
    }

    @Transactional(readOnly = true)
    public Shop getMyShop(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != UserRole.SHOPKEEPER) throw new IllegalArgumentException("Only shopkeepers have shops");
        return shopRepository.findByOwner_Id(user.getId()).orElseThrow(() -> new ResourceNotFoundException("No shop application found"));
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
        if (!category.isActive()) {
            throw new IllegalArgumentException("Choose an active category");
        }

        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Shopkeeper account not found"));
        if (owner.getRole() != UserRole.SHOPKEEPER) {
            throw new IllegalArgumentException("Only shopkeepers can submit a shop");
        }
        Shop shop = shopRepository.findByOwner_Id(owner.getId()).orElse(null);
        if (shop != null && shop.getApprovalStatus() != ShopApprovalStatus.REJECTED) {
            throw new IllegalArgumentException("This shopkeeper already has a shop application");
        }
        if (shop == null) shop = new Shop();
        String oldImagePath = shop.getImagePath();
        shop.setName(request.getName().trim());
        shop.setDescription(request.getDescription());
        shop.setAddress(request.getAddress().trim());
        shop.setContactPhone(request.getContactPhone());
        shop.setContactEmail(request.getContactEmail());
        shop.setCategory(category);
        shop.setOwner(owner);
        shop.setApprovalStatus(ShopApprovalStatus.PENDING);
        shop.setRejectionReason(null);

        if(image != null && !image.isEmpty()){
            String imagePath = imageStorageService.storeImage(image,"shops");

            shop.setImagePath(imagePath);
        }

        Shop savedShop = shopRepository.save(shop);
        if (image != null && !image.isEmpty() && oldImagePath != null
                && !oldImagePath.equals(savedShop.getImagePath())) {
            imageStorageService.deleteImageAfterCommit(oldImagePath);
        }
        return savedShop;
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
    if (!category.isActive()) {
        throw new IllegalArgumentException("Choose an active category");
    }

    boolean requiresReapproval = shop.getApprovalStatus() == ShopApprovalStatus.REJECTED
            || !shop.getName().equals(request.getName().trim())
            || !shop.getAddress().equals(request.getAddress().trim())
            || !java.util.Objects.equals(shop.getDescription(), request.getDescription())
            || !java.util.Objects.equals(shop.getCategory().getId(), category.getId());

    shop.setName(request.getName().trim());
    shop.setDescription(request.getDescription());
    shop.setAddress(request.getAddress().trim());
    shop.setContactPhone(request.getContactPhone());
    shop.setContactEmail(request.getContactEmail());
    shop.setCategory(category);

    String oldImagePath = shop.getImagePath();
    if (image != null && !image.isEmpty()) {
        String imagePath =
                imageStorageService.storeImage(image, "shops");
        shop.setImagePath(imagePath);
    }

    if (requiresReapproval) {
        shop.setApprovalStatus(ShopApprovalStatus.PENDING);
        shop.setRejectionReason(null);
    }

    Shop savedShop = shopRepository.save(shop);
    if (image != null && !image.isEmpty() && oldImagePath != null
            && !oldImagePath.equals(savedShop.getImagePath())) {
        imageStorageService.deleteImageAfterCommit(oldImagePath);
    }
    return savedShop;
}

    

    @Transactional(readOnly = true)
    public Page<Shop> getAllShops(int page, int size) {
        return shopRepository.findByApprovalStatusAndActiveTrue(ShopApprovalStatus.APPROVED,
                PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 100), Sort.by("id").descending()));
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
    public Page<Shop> searchShops(String keyword, int page, int size) {
        String normalizedKeyword = SearchText.normalizeAndEscape(keyword, 100);
        return shopRepository.searchPublicShops(ShopApprovalStatus.APPROVED, normalizedKeyword,
                PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 100)));
    }

    @Transactional(readOnly = true)
    public Page<Shop> searchPublicShops(String keyword, int page, int size) {
        return searchShops(keyword, page, size);
    }

    
    @Transactional(readOnly = true)
    public Page<Shop> getShopsByCategory(Long categoryId, int page, int size) {

        if (!categoryRepository.existsById(categoryId)) {
            throw new CategoryNotFoundException(
                    "Category not found with ID: " + categoryId
            );
        }

        return shopRepository.findByCategory_IdAndApprovalStatusAndActiveTrue(categoryId,
                ShopApprovalStatus.APPROVED, PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 100)));
    }
}


