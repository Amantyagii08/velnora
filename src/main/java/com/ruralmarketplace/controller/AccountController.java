package com.ruralmarketplace.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruralmarketplace.dto.ChangePasswordRequest;
import com.ruralmarketplace.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    private final AuthService authService;

    public AccountController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        authService.logout(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        authService.changePassword(jwt.getSubject(), request);
        return ResponseEntity.noContent().build();
    }
}
