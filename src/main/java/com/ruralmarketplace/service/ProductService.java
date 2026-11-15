
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
import com.ruralmarketplace.dto.SetStockRequest;
import com.ruralmarketplace.entity.StockAdjustment;


import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;

import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;


@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final ImageStorageService imageStorageService;
    private final UserRepository userRepository;
    private final StockAdjustmentService stockAdjustmentService;

    @Autowired
    public ProductService(
            ProductRepository productRepository,
            ShopRepository shopRepository,
            ImageStorageService imageStorageService,
            UserRepository userRepository,
            StockAdjustmentService stockAdjustmentService) {
        this.productRepository = productRepository;
        this.shopRepository = shopRepository;
        this.imageStorageService = imageStorageService;
        this.userRepository = userRepository;
        this.stockAdjustmentService = stockAdjustmentService;
    }

    public ProductService(ProductRepository productRepository, ShopRepository shopRepository,
            ImageStorageService imageStorageService, UserRepository userRepository) {
        this(productRepository, shopRepository, imageStorageService, userRepository, null);
    }

    @Transactional
    public Product createProduct(Long shopId, CreateProductRequest request, MultipartFile image, String ownerEmail)
        throws IOException{

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFoundException(
                        "Shop not found with ID: " + shopId
                ));
        User owner = requireApprovedActiveOwner(shop, ownerEmail);

        Product product = new Product();
        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setBrand(request.getBrand());
        product.setVariant(request.getVariant());
        setPackSize(product, request.getPackSizeValue(), request.getPackSizeUnit());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setAvailable(true);
        product.setShop(shop);

        if(image !=null&& !image.isEmpty()){
                String imagePath = imageStorageService.storeImage(image, "products");

                product.setImagePath(imagePath);
        }



        Product saved = productRepository.save(product);
        if (stockAdjustmentService != null) {
            stockAdjustmentService.record(saved, owner, 0, saved.getStockQuantity(), "INITIAL_STOCK");
        }
        return saved;
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
    public Page<Product> getProductsByShop(Long shopId, int page, int size) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFoundException("Shop not found with ID: " + shopId));
        if (shop.getApprovalStatus() != ShopApprovalStatus.APPROVED || !shop.isActive()) {
            throw new ShopNotFoundException("Shop not found with ID: " + shopId);
        }
        return productRepository.findByShop_IdAndActiveTrue(shopId,
                PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 100)));
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with ID: " + productId
                ));
        if (!product.isActive() || !product.getShop().isActive()
                || product.getShop().getApprovalStatus() != ShopApprovalStatus.APPROVED) {
            throw new ProductNotFoundException("Product not found with ID: " + productId);
        }
        return product;
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
    requireApprovedActiveOwner(shop, ownerEmail);

    Product product = productRepository.findByIdForUpdate(productId)
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
    setPackSize(product, request.getPackSizeValue(), request.getPackSizeUnit());
    product.setPrice(request.getPrice());

    if (Boolean.TRUE.equals(product.getDiscountActive())
            && product.getDiscountAmount().compareTo(request.getPrice()) >= 0) {
        throw new IllegalArgumentException("New price must be greater than the active discount");
    }
    if (product.hasActiveClearance()
            && product.getClearanceDiscountAmount().compareTo(request.getPrice()) >= 0) {
        throw new IllegalArgumentException("New price must be greater than the active clearance discount");
    }

    String oldImagePath = product.getImagePath();
    if(image!=null && !image.isEmpty()){
        String imagePath = imageStorageService.storeImage(image,"products");

        product.setImagePath(imagePath);
    }

    Product savedProduct = productRepository.save(product);
    if (image != null && !image.isEmpty() && oldImagePath != null
            && !oldImagePath.equals(savedProduct.getImagePath())) {
        imageStorageService.deleteImageAfterCommit(oldImagePath);
    }
    return savedProduct;
}


    @Transactional(readOnly = true)
    public Page<Product> searchProducts(String keyword, int page, int size) {
        return productRepository.searchPublicProducts(ShopApprovalStatus.APPROVED,
                normalizeSearchKeyword(keyword), PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 100)));
    }

    @Transactional(readOnly = true)
    public Page<Product> searchPublicProducts(String keyword, int page, int size) {
        return searchProducts(keyword, page, size);
    }

    @Transactional(readOnly = true)
    public Page<Product> searchProductsByShop(Long shopId, String keyword, int page, int size) {

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFoundException(
                        "Shop not found with ID: " + shopId
                ));

        if (shop.getApprovalStatus() != ShopApprovalStatus.APPROVED || !shop.isActive()) {
            throw new ShopNotFoundException("Shop not found with ID: " + shopId);
        }

        return productRepository.searchPublicProductsByShop(shop.getId(), ShopApprovalStatus.APPROVED,
                normalizeSearchKeyword(keyword), PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 100)));
    }

     
        @Transactional
        public void deleteProduct(Long shopId, Long productId, String ownerEmail) {

    Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ShopNotFoundException(
                    "Shop not found with ID: " + shopId
            ));
    requireApprovedActiveOwner(shop, ownerEmail);

    Product product = productRepository.findByIdForUpdate(productId)
            .orElseThrow(() -> new ProductNotFoundException(
                    "Product not found with ID: " + productId
            ));

    if (!product.getShop().getId().equals(shop.getId())) {
        throw new ProductNotFoundException(
                "Product not found in shop with ID: " + shopId
        );
    }

    product.setActive(false);
    productRepository.save(product);
}


@Transactional(readOnly = true)
public Integer getProductStock(Long shopId, Long productId, String ownerEmail) {

    Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ShopNotFoundException(
                    "Shop not found with ID: " + shopId
            ));
    requireApprovedActiveOwner(shop, ownerEmail);

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
    requireApprovedActiveOwner(shop, ownerEmail);

    Product product = productRepository.findByIdForUpdate(productId)
            .orElseThrow(() -> new ProductNotFoundException(
                    "Product not found with ID: " + productId
            ));

    if (!product.getShop().getId().equals(shop.getId())) {
        throw new ProductNotFoundException(
                "Product not found in shop with ID: " + shopId
        );
    }

    if (product.getStockQuantity() == null) {
        throw new IllegalStateException("Product stock has not been initialized");
    }
    try {
        int previousQuantity = product.getStockQuantity();
        product.setStockQuantity(Math.addExact(previousQuantity, request.getQuantity()));
        Product saved = productRepository.save(product);
        if (stockAdjustmentService != null) {
            User actor = userRepository.findByEmail(ownerEmail).orElseThrow(() -> new com.ruralmarketplace.exception.ResourceNotFoundException("User not found"));
            stockAdjustmentService.record(saved, actor, previousQuantity, saved.getStockQuantity(), "RESTOCK");
        }
        return saved;
    } catch (ArithmeticException ex) {
        throw new IllegalArgumentException("Stock quantity exceeds the supported limit", ex);
    }
}

@Transactional
public Product setStock(Long shopId, Long productId, SetStockRequest request, String ownerEmail) {
    Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ShopNotFoundException("Shop not found with ID: " + shopId));
    requireApprovedActiveOwner(shop, ownerEmail);
    Product product = productRepository.findByIdForUpdate(productId)
            .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));
    if (!product.getShop().getId().equals(shopId)) {
        throw new ProductNotFoundException("Product not found in shop with ID: " + shopId);
    }
    int previousQuantity = product.getStockQuantity();
    product.setStockQuantity(request.getQuantity());
    Product saved = productRepository.save(product);
    if (stockAdjustmentService != null) {
        User actor = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new com.ruralmarketplace.exception.ResourceNotFoundException("User not found"));
        String reason = request.getReason() == null || request.getReason().isBlank()
                ? "MANUAL_SET" : request.getReason().trim();
        stockAdjustmentService.record(saved, actor, previousQuantity, saved.getStockQuantity(), reason);
    }
    return saved;
}

@Transactional(readOnly = true)
public Page<StockAdjustment> getStockHistory(Long shopId, Long productId, String ownerEmail,
        int page, int size) {
    Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ShopNotFoundException("Shop not found with ID: " + shopId));
    requireApprovedActiveOwner(shop, ownerEmail);
    Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));
    if (!product.getShop().getId().equals(shopId)) {
        throw new ProductNotFoundException("Product not found in shop with ID: " + shopId);
    }
    return stockAdjustmentService.history(productId, PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 100)));
}

@Transactional
public Product setAvailability(Long shopId, Long productId, boolean available, String ownerEmail) {
    Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new ShopNotFoundException("Shop not found with ID: " + shopId));
    requireApprovedActiveOwner(shop, ownerEmail);
    Product product = productRepository.findByIdForUpdate(productId).orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));
    if (!product.getShop().getId().equals(shopId)) throw new ProductNotFoundException("Product not found in shop with ID: " + shopId);
    if (available && (product.getStockQuantity() == null || product.getStockQuantity() <= 0)) {
        throw new IllegalArgumentException("A product needs stock before it can be marked available");
    }
    product.setAvailable(available);
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
    requireApprovedActiveOwner(shop, ownerEmail);

    Product product = productRepository.findByIdForUpdate(productId)
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

    if (product.hasActiveClearance()) {
        throw new IllegalArgumentException("Remove the clearance deal before applying a regular discount");
    }
    product.setClearanceActive(false);
    product.setDiscountAmount(request.getDiscountAmount());
    product.setDiscountActive(true);

    return productRepository.save(product);
}

@Transactional
public Product setClearance(Long shopId, Long productId, BigDecimal amount, java.time.LocalDateTime expiresAt, String ownerEmail) {
    Shop shop = shopRepository.findById(shopId).orElseThrow(() -> new ShopNotFoundException("Shop not found with ID: " + shopId));
    requireApprovedActiveOwner(shop, ownerEmail);
    if (shop.getApprovalStatus() != ShopApprovalStatus.APPROVED || !shop.isActive()) throw new IllegalArgumentException("Shop is not active and approved");
    Product product = productRepository.findByIdForUpdate(productId).orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));
    if (!product.getShop().getId().equals(shop.getId())) throw new ProductNotFoundException("Product not found in shop with ID: " + shopId);
    if (!product.isActive() || !product.isManuallyAvailable() || product.getStockQuantity() == null
            || product.getStockQuantity() <= 0) {
        throw new IllegalArgumentException("Clearance deals require an active, available product with stock");
    }
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
    requireApprovedActiveOwner(shop, ownerEmail);
    Product product = productRepository.findByIdForUpdate(productId).orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + productId));
    if (!product.getShop().getId().equals(shopId)) throw new ProductNotFoundException("Product not found in shop with ID: " + shopId);
    product.setClearanceActive(false);
    return productRepository.save(product);
}

@Transactional(readOnly = true)
public List<Product> getClearanceDeals() {
    return getClearanceDeals(50);
}

@Transactional(readOnly = true)
public List<Product> getClearanceDeals(int limit) {
    int boundedLimit = Math.max(1, Math.min(limit, 50));
    return productRepository.findPublicClearanceDeals(ShopApprovalStatus.APPROVED,
            org.springframework.data.domain.PageRequest.of(0, boundedLimit)).getContent();
}

@Transactional(readOnly = true)
public Page<Product> getClearanceDealsPage(int page, int size) {
    return productRepository.findPublicClearanceDeals(ShopApprovalStatus.APPROVED,
            PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 100)));
}

@Transactional(readOnly = true)
public Page<Product> getShopClearanceDeals(Long shopId, String ownerEmail, int page, int size) {
    Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ShopNotFoundException("Shop not found with ID: " + shopId));
    requireApprovedActiveOwner(shop, ownerEmail);
    return productRepository.findPublicClearanceDealsByShop(shopId, ShopApprovalStatus.APPROVED,
            PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 100)));
}

@Transactional(readOnly = true)
public Page<Product> compareProducts(String brand, String name, String variant, BigDecimal packSizeValue,
        String packSizeUnit, int page, int size) {
    if (brand == null || brand.isBlank() || name == null || name.isBlank() || variant == null
            || variant.isBlank() || packSizeValue == null || packSizeUnit == null || packSizeUnit.isBlank()) {
        throw new IllegalArgumentException("Brand, product name, variant, pack size value, and pack size unit are required for comparison");
    }
    if (brand.length() > 100 || name.length() > 100 || variant.length() > 100 || packSizeUnit.length() > 20) {
        throw new IllegalArgumentException("Comparison values exceed the allowed length");
    }
    return productRepository.findComparableProducts(ShopApprovalStatus.APPROVED, brand.trim(), name.trim(),
            variant.trim(), packSizeValue, packSizeUnit.trim(),
            PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 100)));
}

private String normalizeSearchKeyword(String keyword) {
    if (keyword == null || keyword.isBlank()) return null;
    if (keyword.length() > 100) throw new IllegalArgumentException("Search keyword cannot exceed 100 characters");
    return keyword.trim().toLowerCase(java.util.Locale.ROOT)
            .replace("!", "!!").replace("%", "!%").replace("_", "!_");
}

private User requireOwner(Shop shop, String email) {
    User user = userRepository.findByEmail(email).orElseThrow(() -> new com.ruralmarketplace.exception.ResourceNotFoundException("User not found"));
    if (shop.getOwner() == null || !shop.getOwner().getId().equals(user.getId())) throw new AccessDeniedException("You can only manage your own shop");
    return user;
}

private User requireApprovedActiveOwner(Shop shop, String email) {
    User owner = requireOwner(shop, email);
    if (shop.getApprovalStatus() != ShopApprovalStatus.APPROVED || !shop.isActive()) {
        throw new AccessDeniedException("Only approved active shops can manage products");
    }
    return owner;
}

private void setPackSize(Product product, BigDecimal value, String unit) {
    if ((value == null) != (unit == null || unit.isBlank())) {
        throw new IllegalArgumentException("Pack size value and unit must be provided together");
    }
    if (value == null) {
        product.setPackSizeValue(null);
        product.setPackSizeUnit(null);
        return;
    }
    product.setPackSizeValue(value.stripTrailingZeros());
    product.setPackSizeUnit(unit.trim().toLowerCase(java.util.Locale.ROOT));
}

@Transactional
public Product removeDiscount(Long shopId, Long productId, String ownerEmail) {

    Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ShopNotFoundException(
                    "Shop not found with ID: " + shopId
            ));
    requireApprovedActiveOwner(shop, ownerEmail);

    Product product = productRepository.findByIdForUpdate(productId)
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

public Page<Product> filterAndSortProducts(
        Long shopId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean available,
        boolean discountedOnly,
        String sortBy,
        int page,
        int size) {

   
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
            ShopApprovalStatus.APPROVED,
            shopId,
            minPrice,
            maxPrice,
            available,
            discountedOnly,
            effectiveSortBy,
            PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 100))
    );
}





}

