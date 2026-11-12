
package com.ruralmarketplace.service;

import com.ruralmarketplace.dto.AddStockRequest;
import com.ruralmarketplace.dto.ApplyDiscountRequest;
import com.ruralmarketplace.dto.CreateProductRequest;
import com.ruralmarketplace.entity.Product;
import com.ruralmarketplace.entity.Shop;
import com.ruralmarketplace.entity.ShopApprovalStatus;
import com.ruralmarketplace.entity.User;
import com.ruralmarketplace.exception.ProductNotFoundException;
import com.ruralmarketplace.exception.ShopNotFoundException;
import com.ruralmarketplace.repository.ProductRepository;
import com.ruralmarketplace.repository.ShopRepository;
import com.ruralmarketplace.repository.UserRepository;
import com.ruralmarketplace.service.image.ImageStorageService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruralmarketplace.dto.UpdateProductRequest;


import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;

import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;


@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final ImageStorageService imageStorageService;
    private final UserRepository userRepository;

    public ProductService(
            ProductRepository productRepository,
            ShopRepository shopRepository,
            ImageStorageService imageStorageService,
            UserRepository userRepository) {
        this.productRepository = productRepository;
        this.shopRepository = shopRepository;
        this.imageStorageService = imageStorageService;
        this.userRepository = userRepository;
    }

    @Transactional
    public Product createProduct(Long shopId, CreateProductRequest request, MultipartFile image)
        throws IOException{

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFoundException(
                        "Shop not found with ID: " + shopId
                ));
        if (shop.getApprovalStatus() != ShopApprovalStatus.APPROVED || !shop.isActive()) throw new IllegalArgumentException("Only approved active shops can add products");

        Product product = new Product();
        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setBrand(request.getBrand());
        product.setVariant(request.getVariant());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setAvailable(true);
        product.setShop(shop);

        if(image !=null&& !image.isEmpty()){
                String imagePath = imageStorageService.storeImage(image, "products");

                product.setImagePath(imagePath);
        }



        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByShop(Long shopId) {

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFoundException(
                        "Shop not found with ID: " + shopId
                ));

        if (shop.getApprovalStatus() != ShopApprovalStatus.APPROVED || !shop.isActive()) throw new ShopNotFoundException("Shop not found with ID: " + shopId);
        return productRepository.findByShop_Id(shop.getId()).stream().filter(Product::isActive).toList();
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long productId) {

        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with ID: " + productId
                ));
    }

    
    @Transactional
public Product updateProduct(
        Long shopId,
        Long productId,
        UpdateProductRequest request,
        MultipartFile image, String ownerEmail) throws IOException {

   
    Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ShopNotFoundException(
                    "Shop not found with ID: " + shopId
            ));
    requireOwner(shop, ownerEmail);

    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(
                    "Product not found with ID: " + productId
            ));

    if (!product.getShop().getId().equals(shop.getId())) {
        throw new ProductNotFoundException(
                "Product not found in shop with ID: " + shopId
        );
    }

    
    product.setName(request.getName().trim());
    product.setDescription(request.getDescription());
    product.setBrand(request.getBrand());
    product.setVariant(request.getVariant());
    product.setPrice(request.getPrice());

    if(image!=null && !image.isEmpty()){
        String imagePath = imageStorageService.storeImage(image,"products");

        product.setImagePath(imagePath);
    }

    return productRepository.save(product);
}


    @Transactional(readOnly = true)
    public List<Product> searchProducts(String keyword) {

        if (keyword == null || keyword.isBlank()) {
        return productRepository.findAll().stream()
                .filter(Product::isActive)
                .filter(p -> p.getShop().getApprovalStatus() == ShopApprovalStatus.APPROVED && p.getShop().isActive())
                .toList();
        }

        return productRepository.findByNameContainingIgnoreCase(keyword.trim()).stream()
                .filter(Product::isActive)
                .filter(p -> p.getShop().getApprovalStatus() == ShopApprovalStatus.APPROVED && p.getShop().isActive())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Product> searchProductsByShop(
            Long shopId, String keyword) {

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFoundException(
                        "Shop not found with ID: " + shopId
                ));

        if (keyword == null || keyword.isBlank()) {
            return productRepository.findByShop_Id(shop.getId()).stream().filter(Product::isActive).toList();
        }

        return productRepository.findByShop_IdAndNameContainingIgnoreCase(shop.getId(), keyword.trim()).stream().filter(Product::isActive).toList();
    }

     
        @Transactional
        public void deleteProduct(Long shopId, Long productId, String ownerEmail) {

    Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ShopNotFoundException(
                    "Shop not found with ID: " + shopId
            ));
    requireOwner(shop, ownerEmail);

    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(
                    "Product not found with ID: " + productId
            ));

    if (!product.getShop().getId().equals(shop.getId())) {
        throw new ProductNotFoundException(
                "Product not found in shop with ID: " + shopId
        );
    }

    productRepository.delete(product);
}


@Transactional(readOnly = true)
public Integer getProductStock(Long shopId, Long productId, String ownerEmail) {

    Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ShopNotFoundException(
                    "Shop not found with ID: " + shopId
            ));
    requireOwner(shop, ownerEmail);

    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(
                    "Product not found with ID: " + productId
            ));

    if (!product.getShop().getId().equals(shop.getId())) {
        throw new ProductNotFoundException(
                "Product not found in shop with ID: " + shopId
        );
    }

    return product.getStockQuantity();
}

@Transactional
public Product addStock(Long shopId, Long productId, AddStockRequest request, String ownerEmail) {

    Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ShopNotFoundException(
                    "Shop not found with ID: " + shopId
            ));
    requireOwner(shop, ownerEmail);

    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(
                    "Product not found with ID: " + productId
            ));

    if (!product.getShop().getId().equals(shop.getId())) {
        throw new ProductNotFoundException(
                "Product not found in shop with ID: " + shopId
        );
    }

    product.setStockQuantity(
            product.getStockQuantity() + request.getQuantity()
    );
    product.setAvailable(product.getStockQuantity() > 0);

    return productRepository.save(product);
}


@Transactional
public Product applyDiscount(
        Long shopId,
        Long productId,
        ApplyDiscountRequest request, String ownerEmail) {

    Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ShopNotFoundException(
                    "Shop not found with ID: " + shopId
            ));
    requireOwner(shop, ownerEmail);

    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(
                    "Product not found with ID: " + productId
            ));

    if (!product.getShop().getId().equals(shop.getId())) {
        throw new ProductNotFoundException(
                "Product not found in shop with ID: " + shopId
        );
    }

    if (request.getDiscountAmount().compareTo(product.getPrice()) >= 0) {
        throw new IllegalArgumentException(
                "Discount must be less than the product price"
        );
    }

    product.setClearanceActive(false);
    product.setDiscountAmount(request.getDiscountAmount());
    product.setDiscountActive(true);

    return productRepository.save(product);
}

@Transactional
public Product setClearance(Long shopId, Long productId, BigDecimal amount, java.time.LocalDateTime expiresAt, String ownerEmail) {
    Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new ShopNotFoundException("Shop not found with ID: " + shopId));
    requireOwner(shop, ownerEmail);
    if (shop.getApprovalStatus() != ShopApprovalStatus.APPROVED || !shop.isActive()) throw new IllegalArgumentException("Shop is not active and approved");
    Product product = productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));
    if (!product.getShop().getId().equals(shop.getId())) throw new ProductNotFoundException("Product not found in shop with ID: " + shopId);
    BigDecimal alreadyDiscountedPrice = Boolean.TRUE.equals(product.getDiscountActive())
            ? product.getPrice().subtract(product.getDiscountAmount()) : product.getPrice();
    if (amount == null || amount.signum() <= 0 || amount.compareTo(alreadyDiscountedPrice) >= 0) throw new IllegalArgumentException("Clearance discount must be positive and less than the current product price");
    if (expiresAt != null && !expiresAt.isAfter(java.time.LocalDateTime.now())) throw new IllegalArgumentException("Clearance expiry must be in the future");
    product.setClearanceDiscountAmount(amount);
    product.setClearanceExpiresAt(expiresAt);
    product.setDiscountAmount(BigDecimal.ZERO);
    product.setDiscountActive(false);
    product.setClearanceActive(true);
    return productRepository.save(product);
}

@Transactional
public Product removeClearance(Long shopId, Long productId, String ownerEmail) {
    Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new ShopNotFoundException("Shop not found with ID: " + shopId));
    requireOwner(shop, ownerEmail);
    Product product = productRepository.findById(productId).orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));
    if (!product.getShop().getId().equals(shopId)) throw new ProductNotFoundException("Product not found in shop with ID: " + shopId);
    product.setClearanceActive(false);
    return productRepository.save(product);
}

@Transactional(readOnly = true)
public List<Product> getClearanceDeals() {
    return productRepository.findAll().stream().filter(Product::isActive)
            .filter(p -> p.getShop().getApprovalStatus() == ShopApprovalStatus.APPROVED && p.getShop().isActive())
            .filter(p -> p.isClearanceActive() && (p.getClearanceExpiresAt() == null || p.getClearanceExpiresAt().isAfter(java.time.LocalDateTime.now())))
            .toList();
}

@Transactional(readOnly = true)
public List<Map<String, Object>> compareProducts(String brand, String variant) {
    return productRepository.findAll().stream()
            .filter(Product::isActive)
            .filter(p -> p.getShop().isActive() && p.getShop().getApprovalStatus() == ShopApprovalStatus.APPROVED)
            .filter(p -> p.getBrand() != null && p.getBrand().equalsIgnoreCase(brand.trim()))
            .filter(p -> p.getVariant() != null && p.getVariant().equalsIgnoreCase(variant.trim()))
            .map(p -> {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("productId", p.getId()); result.put("productName", p.getName()); result.put("brand", p.getBrand());
                result.put("variant", p.getVariant()); result.put("shopId", p.getShop().getId()); result.put("shopName", p.getShop().getName());
                result.put("originalPrice", p.getPrice()); result.put("discountedPrice", p.getCurrentPrice()); result.put("available", Boolean.TRUE.equals(p.getAvailable()) && p.getStockQuantity() > 0);
                return result;
            }).sorted((a, b) -> ((BigDecimal) a.get("discountedPrice")).compareTo((BigDecimal) b.get("discountedPrice")))
            .toList();
}

private void requireOwner(Shop shop, String email) {
    User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
    if (!shop.getOwner().getId().equals(user.getId())) throw new IllegalArgumentException("You can only manage your own shop");
}

@Transactional
public Product removeDiscount(Long shopId, Long productId, String ownerEmail) {

    Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ShopNotFoundException(
                    "Shop not found with ID: " + shopId
            ));
    requireOwner(shop, ownerEmail);

    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(
                    "Product not found with ID: " + productId
            ));

    if (!product.getShop().getId().equals(shop.getId())) {
        throw new ProductNotFoundException(
                "Product not found in shop with ID: " + shopId
        );
    }

    product.setDiscountAmount(java.math.BigDecimal.ZERO);
    product.setDiscountActive(false);

    return productRepository.save(product);
}

public List<Product> filterAndSortProducts(
        Long shopId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean available,
        boolean discountedOnly,
        String sortBy) {

   
    if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
        throw new IllegalArgumentException(
                "Minimum price cannot be negative");
    }

    if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
        throw new IllegalArgumentException(
                "Maximum price cannot be negative");
    }

    if (minPrice != null && maxPrice != null
            && minPrice.compareTo(maxPrice) > 0) {
        throw new IllegalArgumentException(
                "Minimum price cannot exceed maximum price");
    }

   
    Set<String> allowedSortOptions = Set.of(
            "PRICE_LOW_TO_HIGH",
            "PRICE_HIGH_TO_LOW",
            "TOP_DISCOUNT"
    );

    if (sortBy != null && !sortBy.isBlank()
            && !allowedSortOptions.contains(sortBy)) {
        throw new IllegalArgumentException(
                "Invalid sorting option: " + sortBy);
    }

    String effectiveSortBy =
            (sortBy == null || sortBy.isBlank())
                    ? "DEFAULT"
                    : sortBy;

    
    if (shopId != null) {
        shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFoundException("Shop not found with id "+shopId));
    }

    return productRepository.findProductsWithFilters(
            shopId,
            minPrice,
            maxPrice,
            available,
            discountedOnly,
            effectiveSortBy
    ).stream().filter(Product::isActive)
            .filter(p -> p.getShop().isActive() && p.getShop().getApprovalStatus() == ShopApprovalStatus.APPROVED)
            .toList();
}





}

