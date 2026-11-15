package com.ruralmarketplace.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.ruralmarketplace.entity.*;
import com.ruralmarketplace.repository.*;
import com.ruralmarketplace.service.CategoryService;
import com.ruralmarketplace.dto.CategoryRequest;
import jakarta.validation.Valid;
import com.ruralmarketplace.exception.ResourceNotFoundException;
import com.ruralmarketplace.dto.AdminUserResponse;
import com.ruralmarketplace.dto.AdminShopResponse;
import com.ruralmarketplace.dto.AdminProductResponse;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserRepository users;
    private final ShopRepository shops;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final CategoryService categories;
    private final CategoryRepository categoryRepository;
    public AdminController(UserRepository users, ShopRepository shops, ProductRepository products, OrderRepository orders, CategoryService categories, CategoryRepository categoryRepository) {
        this.users = users; this.shops = shops; this.products = products; this.orders = orders; this.categories = categories; this.categoryRepository = categoryRepository;
    }
    @GetMapping("/dashboard") public Map<String,Long> dashboard() {
        Map<String,Long> result = new LinkedHashMap<>();
        result.put("customers", users.countByRole(UserRole.CUSTOMER));
        result.put("shopkeepers", users.countByRole(UserRole.SHOPKEEPER));
        result.put("shops", shops.count());
        result.put("approvedShops", shops.countByApprovalStatus(ShopApprovalStatus.APPROVED));
        result.put("pendingShops", shops.countByApprovalStatus(ShopApprovalStatus.PENDING));
        result.put("products", products.count()); result.put("orders", orders.count());
        result.put("completedOrders", orders.countByStatus(OrderStatus.COMPLETED));
        result.put("cancelledOrders", orders.countByStatus(OrderStatus.CANCELLED)); return result;
    }
    @GetMapping({"/users", "/users/page"}) public Page<AdminUserResponse> usersPage(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return users.searchUsers(role, normalizeKeyword(keyword), pageRequest(page, size)).map(AdminUserResponse::from);
    }
    @GetMapping("/users/search") public Page<AdminUserResponse> searchUsers(@RequestParam String keyword,
            @RequestParam(required = false) UserRole role, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return users.searchUsers(role, normalizeKeyword(keyword), pageRequest(page, size)).map(AdminUserResponse::from);
    }
    @GetMapping("/users/{userId}") public AdminUserResponse userDetails(@PathVariable Long userId) {
        return AdminUserResponse.from(users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }
    @PatchMapping("/users/{userId}/suspension") public AdminUserResponse suspendUser(@PathVariable Long userId, @RequestParam boolean suspended) {
        User user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() == UserRole.ADMIN) throw new IllegalArgumentException("Admin accounts cannot be suspended through this endpoint");
        user.setSuspended(suspended); return AdminUserResponse.from(users.save(user));
    }
    @GetMapping({"/shops", "/shops/page"}) public Page<AdminShopResponse> shopsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return shops.findAllBy(pageRequest(page, size)).map(AdminShopResponse::from);
    }
    @GetMapping("/categories") public Page<com.ruralmarketplace.dto.AdminCategoryResponse> allCategories(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return categoryRepository.findAll(pageRequest(page, size)).map(com.ruralmarketplace.dto.AdminCategoryResponse::from);
    }
    @PatchMapping("/shops/{shopId}/approval") public AdminShopResponse approveShop(@PathVariable Long shopId,
            @RequestParam ShopApprovalStatus status, @RequestParam(required = false) String rejectionReason) {
        Shop shop = shops.findById(shopId).orElseThrow(() -> new ResourceNotFoundException("Shop not found"));
        if (status == ShopApprovalStatus.PENDING) throw new IllegalArgumentException("Approval status must be APPROVED or REJECTED");
        if (shop.getApprovalStatus() != ShopApprovalStatus.PENDING) {
            throw new IllegalStateException("Only pending shop applications can be approved or rejected");
        }
        if (status == ShopApprovalStatus.REJECTED && (rejectionReason == null || rejectionReason.isBlank())) {
            throw new IllegalArgumentException("A rejection reason is required");
        }
        if (rejectionReason != null && rejectionReason.length() > 500) {
            throw new IllegalArgumentException("Rejection reason cannot exceed 500 characters");
        }
        shop.setApprovalStatus(status);
        shop.setRejectionReason(status == ShopApprovalStatus.REJECTED ? rejectionReason.trim() : null);
        return AdminShopResponse.from(shops.save(shop));
    }
    @PatchMapping("/shops/{shopId}/suspension") public AdminShopResponse suspendShop(@PathVariable Long shopId, @RequestParam boolean suspended) {
        Shop shop = shops.findById(shopId).orElseThrow(() -> new ResourceNotFoundException("Shop not found"));
        shop.setActive(!suspended); return AdminShopResponse.from(shops.save(shop));
    }
    @GetMapping({"/products", "/products/page"}) public Page<AdminProductResponse> productsPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return products.searchAllProducts(normalizeKeyword(keyword), pageRequest(page, size)).map(AdminProductResponse::from);
    }
    @GetMapping("/products/search") public Page<AdminProductResponse> searchProducts(@RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return products.searchAllProducts(normalizeKeyword(keyword), pageRequest(page, size)).map(AdminProductResponse::from);
    }
    @GetMapping("/products/{productId}") public AdminProductResponse productDetails(@PathVariable Long productId) {
        return AdminProductResponse.from(products.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product not found")));
    }
    @PatchMapping("/products/{productId}/active") public AdminProductResponse setProductActive(@PathVariable Long productId, @RequestParam boolean active) {
        Product product = products.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setActive(active); return AdminProductResponse.from(products.save(product));
    }
    @GetMapping({"/orders", "/orders/page"}) @Transactional(readOnly = true)
    public Page<OrderController.OrderResponse> ordersPage(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String normalizedKeyword = normalizeKeyword(keyword);
        Long orderId = parseOrderId(normalizedKeyword);
        return orders.searchOrders(status, normalizedKeyword, orderId, pageRequest(page, size))
                .map(OrderController.OrderResponse::new);
    }
    @GetMapping("/orders/search") @Transactional(readOnly = true)
    public Page<OrderController.OrderResponse> searchOrders(@RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String keyword, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String normalizedKeyword = normalizeKeyword(keyword);
        return orders.searchOrders(status, normalizedKeyword, parseOrderId(normalizedKeyword), pageRequest(page, size))
                .map(OrderController.OrderResponse::new);
    }
    @GetMapping("/orders/{orderId}") public OrderController.OrderResponse orderDetails(@PathVariable Long orderId) {
        return new OrderController.OrderResponse(orders.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found")));
    }
    @PostMapping(value = "/categories", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public com.ruralmarketplace.dto.AdminCategoryResponse createCategory(@Valid @ModelAttribute CategoryRequest category, @RequestParam(required = false) MultipartFile image) throws IOException {
        return com.ruralmarketplace.dto.AdminCategoryResponse.from(categories.createCategory(category, image));
    }
    @PutMapping(value = "/categories/{categoryId}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public com.ruralmarketplace.dto.AdminCategoryResponse updateCategory(@PathVariable Long categoryId, @Valid @ModelAttribute CategoryRequest request, @RequestParam(required = false) MultipartFile image) throws IOException {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setName(request.getName().trim());
        String oldImagePath = category.getImagePath();
        if (image != null && !image.isEmpty()) category.setImagePath(categories.storeCategoryImage(image));
        Category savedCategory = categoryRepository.save(category);
        if (image != null && !image.isEmpty() && oldImagePath != null
                && !oldImagePath.equals(savedCategory.getImagePath())) categories.deleteStoredImage(oldImagePath);
        return com.ruralmarketplace.dto.AdminCategoryResponse.from(savedCategory);
    }
    @DeleteMapping("/categories/{categoryId}") public void deactivateCategory(@PathVariable Long categoryId) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setActive(false); categoryRepository.save(category);
    }
    @PatchMapping("/categories/{categoryId}/active") public com.ruralmarketplace.dto.AdminCategoryResponse setCategoryActive(@PathVariable Long categoryId, @RequestParam boolean active) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setActive(active);
        return com.ruralmarketplace.dto.AdminCategoryResponse.from(categoryRepository.save(category));
    }

    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 100));
    }

    private static String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private static Long parseOrderId(String keyword) {
        if (keyword == null || !keyword.chars().allMatch(Character::isDigit)) return null;
        try {
            return Long.valueOf(keyword);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
