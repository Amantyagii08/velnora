package com.ruralmarketplace.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruralmarketplace.controller.OrderController.OrderResponse;
import com.ruralmarketplace.dto.AddCartItemRequest;
import com.ruralmarketplace.dto.CartCheckoutRequest;
import com.ruralmarketplace.dto.CartItemResponse;
import com.ruralmarketplace.dto.UpdateCartQuantityRequest;
import com.ruralmarketplace.service.CartService;

import jakarta.validation.Valid;

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
    @PatchMapping("/items/{itemId}") public List<CartItemResponse> updateQuantity(@PathVariable Long itemId,
            @Valid @RequestBody UpdateCartQuantityRequest request, @AuthenticationPrincipal Jwt jwt) {
        return cartService.updateQuantity(jwt.getSubject(), itemId, request.getQuantity());
    }
    @PostMapping("/checkout") public OrderResponse checkout(@Valid @RequestBody CartCheckoutRequest request, @AuthenticationPrincipal Jwt jwt) {
        return new OrderResponse(cartService.checkout(jwt.getSubject(), request));
    }
    @DeleteMapping("/items/{itemId}")
     public void remove(@PathVariable Long itemId, @AuthenticationPrincipal Jwt jwt) { cartService.remove(jwt.getSubject(), itemId); }
    @DeleteMapping public void clear(@AuthenticationPrincipal Jwt jwt) { cartService.clear(jwt.getSubject()); }
}
