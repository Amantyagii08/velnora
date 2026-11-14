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
import com.ruralmarketplace.exception.ResourceNotFoundException;
import com.ruralmarketplace.dto.CartCheckoutRequest;
import com.ruralmarketplace.dto.CreateOrderRequest;
import com.ruralmarketplace.dto.CartItemResponse;

@Service
public class CartService {
    private final CartItemRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;
    public CartService(CartItemRepository cartRepository, ProductRepository productRepository, UserRepository userRepository, OrderService orderService) {
        this.cartRepository = cartRepository; this.productRepository = productRepository; this.userRepository = userRepository; this.orderService = orderService;
    }
    @Transactional
    public List<CartItemResponse> add(String email, AddCartItemRequest request) {
        User customer = customer(email);
        Product product = productRepository.findById(request.getProductId()).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (!product.isActive() || !product.getShop().isActive() || product.getShop().getApprovalStatus() != ShopApprovalStatus.APPROVED) throw new IllegalArgumentException("Product is unavailable");
        if (!product.isManuallyAvailable() || product.getStockQuantity() == null
                || request.getQuantity() > product.getStockQuantity()) {
            throw new IllegalArgumentException("Requested quantity exceeds available stock");
        }
        List<CartItem> items = cartRepository.findByCustomer_Email(email);
        boolean differentShop = items.stream().anyMatch(item -> !item.getProduct().getShop().getId().equals(product.getShop().getId()));
        if (differentShop && !request.isClearExistingCart()) throw new IllegalStateException("CART_SHOP_MISMATCH: confirm clearing the existing cart to continue");
        if (differentShop) cartRepository.deleteAll(items);
        CartItem item = cartRepository.findByCustomer_EmailAndProduct_Id(email, product.getId()).orElseGet(CartItem::new);
        item.setCustomer(customer); item.setProduct(product); item.setQuantity(request.getQuantity()); cartRepository.save(item);
        return cartRepository.findByCustomer_Email(email).stream().map(CartItemResponse::new).toList();
    }
    @Transactional(readOnly = true)
    public Map<String, Object> get(String email) {
        List<CartItem> items = cartRepository.findByCustomer_Email(email);
        BigDecimal total = items.stream().map(i -> i.getProduct().getCurrentPrice().multiply(BigDecimal.valueOf(i.getQuantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String,Object> result = new LinkedHashMap<>(); result.put("items", items.stream().map(CartItemResponse::new).toList()); result.put("total", total); return result;
    }
    @Transactional
    public Order checkout(String email, CartCheckoutRequest checkoutRequest) {
        List<CartItem> cartItems = cartRepository.findByCustomer_Email(email);
        if (cartItems.isEmpty()) throw new IllegalArgumentException("Cart is empty");
        Long shopId = cartItems.get(0).getProduct().getShop().getId();
        if (cartItems.stream().anyMatch(item -> !item.getProduct().getShop().getId().equals(shopId))) {
            throw new IllegalStateException("Cart contains products from more than one shop");
        }
        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setShopId(shopId);
        orderRequest.setFulfillmentType(checkoutRequest.getFulfillmentType());
        orderRequest.setDeliveryAddress(checkoutRequest.getDeliveryAddress());
        orderRequest.setItems(cartItems.stream().map(item -> {
            CreateOrderRequest.OrderItemRequest line = new CreateOrderRequest.OrderItemRequest();
            line.setProductId(item.getProduct().getId()); line.setQuantity(item.getQuantity()); return line;
        }).toList());
        Order order = orderService.createOrder(orderRequest, email);
        cartRepository.deleteAll(cartItems);
        return order;
    }
    @Transactional public void remove(String email, Long itemId) {
        CartItem item = cartRepository.findById(itemId).orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        if (!item.getCustomer().getEmail().equals(email)) throw new ResourceNotFoundException("Cart item not found");
        cartRepository.delete(item);
    }
    @Transactional public void clear(String email) { cartRepository.deleteByCustomer_Email(email); }
    private User customer(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        if (user.getRole() != UserRole.CUSTOMER) throw new IllegalArgumentException("Only customers can use a cart"); return user;
    }
}
