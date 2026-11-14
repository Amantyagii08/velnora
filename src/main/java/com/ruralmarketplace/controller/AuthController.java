
package com.ruralmarketplace.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import com.ruralmarketplace.dto.auth.AuthResponse;
import com.ruralmarketplace.dto.auth.LoginRequest;
import com.ruralmarketplace.dto.auth.RegisterRequest;
import com.ruralmarketplace.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        AuthResponse response = authService.login(request, httpRequest.getRemoteAddr());

        return ResponseEntity.ok(response);
    }
}
