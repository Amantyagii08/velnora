
package com.ruralmarketplace.service;

import com.ruralmarketplace.dto.AddStockRequest;
import com.ruralmarketplace.dto.ApplyDiscountRequest;
import com.ruralmarketplace.dto.CreateProductRequest;
import com.ruralmarketplace.entity.Product;
import com.ruralmarketplace.entity.Shop;
import com.ruralmarketplace.exception.ProductNotFoundException;
import com.ruralmarketplace.exception.ShopNotFoundException;
import com.ruralmarketplace.repository.ProductRepository;
import com.ruralmarketplace.repository.ShopRepository;
import com.ruralmarketplace.service.image.ImageStorageService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruralmarketplace.dto.UpdateProductRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;


@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final ImageStorageService imageStorageService;

    public ProductService(
            ProductRepository productRepository,
            ShopRepository shopRepository,
            ImageStorageService imageStorageService) {
        this.productRepository = productRepository;
        this.shopRepository = shopRepository;
        this.imageStorageService = imageStorageService;
    }

    @Transactional
    public Product createProduct(Long shopId, CreateProductRequest request, MultipartFile image)
        throws IOException{

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFoundException(
                        "Shop not found with ID: " + shopId
                ));

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

        return productRepository.findByShop_Id(shop.getId());
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
        MultipartFile image) throws IOException {

   
    Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ShopNotFoundException(
                    "Shop not found with ID: " + shopId
            ));

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
            return productRepository.findAll();
        }

        return productRepository.findByNameContainingIgnoreCase(
                keyword.trim()
        );
    }

    @Transactional(readOnly = true)
    public List<Product> searchProductsByShop(
            Long shopId, String keyword) {

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ShopNotFoundException(
                        "Shop not found with ID: " + shopId
                ));

        if (keyword == null || keyword.isBlank()) {
            return productRepository.findByShop_Id(shop.getId());
        }

        return productRepository.findByShop_IdAndNameContainingIgnoreCase(
                shop.getId(), keyword.trim()
        );
    }

     
        @Transactional
        public void deleteProduct(Long shopId, Long productId) {

    Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ShopNotFoundException(
                    "Shop not found with ID: " + shopId
            ));

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
public Integer getProductStock(Long shopId, Long productId) {

    Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ShopNotFoundException(
                    "Shop not found with ID: " + shopId
            ));

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
public Product addStock(Long shopId, Long productId, AddStockRequest request) {

    Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ShopNotFoundException(
                    "Shop not found with ID: " + shopId
            ));

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

    return productRepository.save(product);
}


@Transactional
public Product applyDiscount(
        Long shopId,
        Long productId,
        ApplyDiscountRequest request) {

    Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ShopNotFoundException(
                    "Shop not found with ID: " + shopId
            ));

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

    product.setDiscountAmount(request.getDiscountAmount());
    product.setDiscountActive(true);

    return productRepository.save(product);
}

@Transactional
public Product removeDiscount(Long shopId, Long productId) {

    Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ShopNotFoundException(
                    "Shop not found with ID: " + shopId
            ));

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
    );
}





}

