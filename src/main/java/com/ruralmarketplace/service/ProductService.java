
package com.ruralmarketplace.service;

import com.ruralmarketplace.dto.CreateProductRequest;
import com.ruralmarketplace.entity.Product;
import com.ruralmarketplace.entity.Shop;
import com.ruralmarketplace.exception.ProductNotFoundException;
import com.ruralmarketplace.exception.ShopNotFoundException;
import com.ruralmarketplace.repository.ProductRepository;
import com.ruralmarketplace.repository.ShopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;

    public ProductService(
            ProductRepository productRepository,
            ShopRepository shopRepository) {
        this.productRepository = productRepository;
        this.shopRepository = shopRepository;
    }

    @Transactional
    public Product createProduct(Long shopId, CreateProductRequest request) {

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
}

