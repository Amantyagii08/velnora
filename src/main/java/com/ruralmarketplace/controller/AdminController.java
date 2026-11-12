package com.ruralmarketplace.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.ruralmarketplace.entity.*;
import com.ruralmarketplace.repository.*;
import com.ruralmarketplace.service.CategoryService;
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
    @GetMapping("/users") public List<User> users(@RequestParam(required = false) UserRole role) { return role == null ? users.findAll() : users.findByRole(role); }
    @GetMapping("/users/search") public List<User> searchUsers(@RequestParam String keyword) {
        String query = keyword.toLowerCase();
        return users.findAll().stream().filter(user -> user.getFullName().toLowerCase().contains(query)
                || user.getEmail().toLowerCase().contains(query)).toList();
    }
    @PatchMapping("/users/{userId}/suspension") public User suspendUser(@PathVariable Long userId, @RequestParam boolean suspended) {
        User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRole() == UserRole.ADMIN) throw new IllegalArgumentException("Admin accounts cannot be suspended through this endpoint");
        user.setSuspended(suspended); return users.save(user);
    }
    @GetMapping("/shops") public List<Shop> shops() { return shops.findAll(); }
    @PatchMapping("/shops/{shopId}/approval") public Shop approveShop(@PathVariable Long shopId, @RequestParam ShopApprovalStatus status) {
        Shop shop = shops.findById(shopId).orElseThrow(() -> new IllegalArgumentException("Shop not found"));
        if (status == ShopApprovalStatus.PENDING) throw new IllegalArgumentException("Approval status must be APPROVED or REJECTED");
        shop.setApprovalStatus(status); return shops.save(shop);
    }
    @PatchMapping("/shops/{shopId}/suspension") public Shop suspendShop(@PathVariable Long shopId, @RequestParam boolean suspended) {
        Shop shop = shops.findById(shopId).orElseThrow(() -> new IllegalArgumentException("Shop not found"));
        shop.setActive(!suspended); return shops.save(shop);
    }
    @GetMapping("/products") public List<Product> products() { return products.findAll(); }
    @PatchMapping("/products/{productId}/active") public Product setProductActive(@PathVariable Long productId, @RequestParam boolean active) {
        Product product = products.findById(productId).orElseThrow(() -> new IllegalArgumentException("Product not found"));
        product.setActive(active); return products.save(product);
    }
    @GetMapping("/orders") public List<Order> orders(@RequestParam(required = false) OrderStatus status) {
        return status == null ? orders.findAll() : orders.findByStatusOrderByCreatedAtDesc(status);
    }
    @PostMapping(value = "/categories", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public Category createCategory(@ModelAttribute Category category, @RequestParam(required = false) MultipartFile image) throws IOException {
        return categories.createCategory(category, image);
    }
    @PutMapping(value = "/categories/{categoryId}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public Category updateCategory(@PathVariable Long categoryId, @ModelAttribute Category request, @RequestParam(required = false) MultipartFile image) throws IOException {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new IllegalArgumentException("Category not found"));
        category.setName(request.getName()); category.setActive(request.isActive());
        if (image != null && !image.isEmpty()) category.setImagePath(categories.storeCategoryImage(image));
        return categoryRepository.save(category);
    }
    @DeleteMapping("/categories/{categoryId}") public void deactivateCategory(@PathVariable Long categoryId) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new IllegalArgumentException("Category not found"));
        category.setActive(false); categoryRepository.save(category);
    }
}
