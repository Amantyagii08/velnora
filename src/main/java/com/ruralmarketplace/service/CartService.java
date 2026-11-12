package com.ruralmarketplace.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruralmarketplace.dto.AddCartItemRequest;
import com.ruralmarketplace.entity.*;
import com.ruralmarketplace.repository.*;

@Service
public class CartService {
    private final CartItemRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    public CartService(CartItemRepository cartRepository, ProductRepository productRepository, UserRepository userRepository) {
        this.cartRepository = cartRepository; this.productRepository = productRepository; this.userRepository = userRepository;
    }
    @Transactional
    public List<CartItem> add(String email, AddCartItemRequest request) {
        User customer = customer(email);
        Product product = productRepository.findById(request.getProductId()).orElseThrow(() -> new IllegalArgumentException("Product not found"));
        if (!product.isActive() || !product.getShop().isActive() || product.getShop().getApprovalStatus() != ShopApprovalStatus.APPROVED) throw new IllegalArgumentException("Product is unavailable");
        List<CartItem> items = cartRepository.findByCustomer_Email(email);
        boolean differentShop = items.stream().anyMatch(item -> !item.getProduct().getShop().getId().equals(product.getShop().getId()));
        if (differentShop && !request.isClearExistingCart()) throw new IllegalStateException("CART_SHOP_MISMATCH: confirm clearing the existing cart to continue");
        if (differentShop) cartRepository.deleteAll(items);
        CartItem item = cartRepository.findByCustomer_EmailAndProduct_Id(email, product.getId()).orElseGet(CartItem::new);
        item.setCustomer(customer); item.setProduct(product); item.setQuantity(request.getQuantity()); cartRepository.save(item);
        return cartRepository.findByCustomer_Email(email);
    }
    public Map<String, Object> get(String email) {
        List<CartItem> items = cartRepository.findByCustomer_Email(email);
        BigDecimal total = items.stream().map(i -> i.getProduct().getCurrentPrice().multiply(BigDecimal.valueOf(i.getQuantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String,Object> result = new LinkedHashMap<>(); result.put("items", items); result.put("total", total); return result;
    }
    @Transactional public void remove(String email, Long itemId) {
        CartItem item = cartRepository.findById(itemId).orElseThrow(() -> new IllegalArgumentException("Cart item not found"));
        if (!item.getCustomer().getEmail().equals(email)) throw new IllegalArgumentException("Cart item not found");
        cartRepository.delete(item);
    }
    @Transactional public void clear(String email) { cartRepository.deleteByCustomer_Email(email); }
    private User customer(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        if (user.getRole() != UserRole.CUSTOMER) throw new IllegalArgumentException("Only customers can use a cart"); return user;
    }
}
