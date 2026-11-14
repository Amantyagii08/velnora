package com.ruralmarketplace.controller;

import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.ruralmarketplace.dto.AddCartItemRequest;
import com.ruralmarketplace.dto.CartCheckoutRequest;
import com.ruralmarketplace.entity.CartItem;
import com.ruralmarketplace.dto.CartItemResponse;
import com.ruralmarketplace.controller.OrderController.OrderResponse;
import com.ruralmarketplace.service.CartService;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {
    private final CartService cartService;
    public CartController(CartService cartService) {
         this.cartService = cartService;
         }
    @GetMapping public Map<String,Object> get(@AuthenticationPrincipal Jwt jwt) { return cartService.get(jwt.getSubject()); }
    @PostMapping("/items") public List<CartItemResponse> add(@Valid @RequestBody AddCartItemRequest request, @AuthenticationPrincipal Jwt jwt) { return cartService.add(jwt.getSubject(), request); }
    @PostMapping("/checkout") public OrderResponse checkout(@Valid @RequestBody CartCheckoutRequest request, @AuthenticationPrincipal Jwt jwt) {
        return new OrderResponse(cartService.checkout(jwt.getSubject(), request));
    }
    @DeleteMapping("/items/{itemId}")
     public void remove(@PathVariable Long itemId, @AuthenticationPrincipal Jwt jwt) { cartService.remove(jwt.getSubject(), itemId); }
    @DeleteMapping public void clear(@AuthenticationPrincipal Jwt jwt) { cartService.clear(jwt.getSubject()); }
}
