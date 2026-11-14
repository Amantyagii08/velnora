
package com.ruralmarketplace.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruralmarketplace.dto.auth.AuthResponse;
import com.ruralmarketplace.dto.auth.LoginRequest;
import com.ruralmarketplace.dto.auth.RegisterRequest;
import com.ruralmarketplace.entity.User;
import com.ruralmarketplace.entity.UserRole;
import com.ruralmarketplace.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            LoginAttemptService loginAttemptService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists"
            );
        }

        String phone = request.getPhone() == null || request.getPhone().isBlank()
                ? null : request.getPhone().trim();
        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException(
                    "An account with this phone number already exists"
            );
        }

        User user = new User();

        user.setFullName(request.getFullName());
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );

        String requestedRole = request.getRole() == null
                ? "CUSTOMER" : request.getRole().trim().toUpperCase();
        if (!requestedRole.equals("CUSTOMER") && !requestedRole.equals("SHOPKEEPER")) {
            throw new IllegalArgumentException("Public registration supports CUSTOMER or SHOPKEEPER only");
        }
        user.setRole(UserRole.valueOf(requestedRole));

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(savedUser);

        return new AuthResponse(
                token,
                jwtService.getExpirationSeconds(),
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
    }

    public AuthResponse login(LoginRequest request, String clientAddress) {

        String email = request.getEmail().trim().toLowerCase();
        String attemptKey = email + "|" + clientAddress;
        loginAttemptService.checkAllowed(attemptKey);

        userRepository.findByEmail(email).filter(User::isSuspended).ifPresent(user -> {
            throw new org.springframework.security.authentication.DisabledException("This account is suspended");
        });

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword()));
            loginAttemptService.recordSuccess(attemptKey);
        } catch (org.springframework.security.core.AuthenticationException ex) {
            loginAttemptService.recordFailure(attemptKey);
            throw ex;
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        String token = jwtService.generateToken(user);

        return new AuthResponse(
                token,
                jwtService.getExpirationSeconds(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}
