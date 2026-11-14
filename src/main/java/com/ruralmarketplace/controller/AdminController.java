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
        result.put("customers", (long) users.findByRole(UserRole.CUSTOMER).size());
        result.put("shopkeepers", (long) users.findByRole(UserRole.SHOPKEEPER).size());
        result.put("shops", shops.count());
        result.put("approvedShops", (long) shops.findByApprovalStatus(ShopApprovalStatus.APPROVED).size());
        result.put("pendingShops", (long) shops.findByApprovalStatus(ShopApprovalStatus.PENDING).size());
        result.put("products", products.count()); result.put("orders", orders.count());
        result.put("completedOrders", orders.countByStatus(OrderStatus.COMPLETED));
        result.put("cancelledOrders", orders.countByStatus(OrderStatus.CANCELLED)); return result;
    }
    @GetMapping("/users") public List<AdminUserResponse> users(@RequestParam(required = false) UserRole role) {
        return (role == null ? users.findAll() : users.findByRole(role)).stream().map(AdminUserResponse::from).toList();
    }
    @GetMapping("/users/search") public List<AdminUserResponse> searchUsers(@RequestParam String keyword) {
        String query = keyword.toLowerCase();
        return users.findAll().stream().filter(user -> user.getFullName().toLowerCase().contains(query)
                || user.getEmail().toLowerCase().contains(query)).map(AdminUserResponse::from).toList();
    }
    @GetMapping("/users/{userId}") public AdminUserResponse userDetails(@PathVariable Long userId) {
        return AdminUserResponse.from(users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }
    @PatchMapping("/users/{userId}/suspension") public AdminUserResponse suspendUser(@PathVariable Long userId, @RequestParam boolean suspended) {
        User user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() == UserRole.ADMIN) throw new IllegalArgumentException("Admin accounts cannot be suspended through this endpoint");
        user.setSuspended(suspended); return AdminUserResponse.from(users.save(user));
    }
    @GetMapping("/shops") public List<AdminShopResponse> shops() { return shops.findAll().stream().map(AdminShopResponse::from).toList(); }
    @PatchMapping("/shops/{shopId}/approval") public AdminShopResponse approveShop(@PathVariable Long shopId, @RequestParam ShopApprovalStatus status) {
        Shop shop = shops.findById(shopId).orElseThrow(() -> new ResourceNotFoundException("Shop not found"));
        if (status == ShopApprovalStatus.PENDING) throw new IllegalArgumentException("Approval status must be APPROVED or REJECTED");
        shop.setApprovalStatus(status); return AdminShopResponse.from(shops.save(shop));
    }
    @PatchMapping("/shops/{shopId}/suspension") public AdminShopResponse suspendShop(@PathVariable Long shopId, @RequestParam boolean suspended) {
        Shop shop = shops.findById(shopId).orElseThrow(() -> new ResourceNotFoundException("Shop not found"));
        shop.setActive(!suspended); return AdminShopResponse.from(shops.save(shop));
    }
    @GetMapping("/products") public List<AdminProductResponse> products() { return products.findAll().stream().map(AdminProductResponse::from).toList(); }
    @GetMapping("/products/search") public List<AdminProductResponse> searchProducts(@RequestParam String keyword) {
        String query = keyword.toLowerCase();
        return products.findAll().stream().filter(p -> p.getName().toLowerCase().contains(query)
                || (p.getBrand() != null && p.getBrand().toLowerCase().contains(query))
                || p.getShop().getName().toLowerCase().contains(query)).map(AdminProductResponse::from).toList();
    }
    @GetMapping("/products/{productId}") public AdminProductResponse productDetails(@PathVariable Long productId) {
        return AdminProductResponse.from(products.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product not found")));
    }
    @PatchMapping("/products/{productId}/active") public AdminProductResponse setProductActive(@PathVariable Long productId, @RequestParam boolean active) {
        Product product = products.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setActive(active); return AdminProductResponse.from(products.save(product));
    }
    @GetMapping("/orders") public List<OrderController.OrderResponse> orders(@RequestParam(required = false) OrderStatus status) {
        List<Order> result = status == null ? orders.findAll() : orders.findByStatusOrderByCreatedAtDesc(status);
        return result.stream().map(OrderController.OrderResponse::new).toList();
    }
    @GetMapping("/orders/search") public List<OrderController.OrderResponse> searchOrders(@RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String keyword) {
        return orders.findAll().stream().filter(order -> status == null || order.getStatus() == status)
                .filter(order -> keyword == null || keyword.isBlank()
                        || order.getId().toString().contains(keyword.trim())
                        || order.getCustomerName().toLowerCase().contains(keyword.trim().toLowerCase())
                        || order.getShop().getName().toLowerCase().contains(keyword.trim().toLowerCase()))
                .map(OrderController.OrderResponse::new).toList();
    }
    @GetMapping("/orders/{orderId}") public OrderController.OrderResponse orderDetails(@PathVariable Long orderId) {
        return new OrderController.OrderResponse(orders.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found")));
    }
    @PostMapping(value = "/categories", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public Category createCategory(@Valid @ModelAttribute CategoryRequest category, @RequestParam(required = false) MultipartFile image) throws IOException {
        return categories.createCategory(category, image);
    }
    @PutMapping(value = "/categories/{categoryId}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public Category updateCategory(@PathVariable Long categoryId, @Valid @ModelAttribute CategoryRequest request, @RequestParam(required = false) MultipartFile image) throws IOException {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setName(request.getName().trim());
        String oldImagePath = category.getImagePath();
        if (image != null && !image.isEmpty()) category.setImagePath(categories.storeCategoryImage(image));
        Category savedCategory = categoryRepository.save(category);
        if (image != null && !image.isEmpty() && oldImagePath != null
                && !oldImagePath.equals(savedCategory.getImagePath())) categories.deleteStoredImage(oldImagePath);
        return savedCategory;
    }
    @DeleteMapping("/categories/{categoryId}") public void deactivateCategory(@PathVariable Long categoryId) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setActive(false); categoryRepository.save(category);
    }
    @PatchMapping("/categories/{categoryId}/active") public Category setCategoryActive(@PathVariable Long categoryId, @RequestParam boolean active) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setActive(active);
        return categoryRepository.save(category);
    }
}
