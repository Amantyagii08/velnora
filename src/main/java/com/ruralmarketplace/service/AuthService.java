
package com.ruralmarketplace.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruralmarketplace.dto.auth.AuthResponse;
import com.ruralmarketplace.dto.auth.LoginRequest;
import com.ruralmarketplace.dto.auth.RegisterRequest;
import com.ruralmarketplace.dto.ChangePasswordRequest;
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
    public AuthResponse register(RegisterRequest request, String clientAddress) {
        loginAttemptService.checkRegistrationAllowedAndRecord(clientAddress);

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

        User existingUser = userRepository.findByEmail(email).orElse(null);
        if (existingUser != null && existingUser.isSuspended()) {
            loginAttemptService.recordFailure(attemptKey);
            throw new org.springframework.security.authentication.DisabledException("Account is suspended");
        }

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

        if (!user.isEnabled()) {
            throw new org.springframework.security.authentication.DisabledException("Account is disabled");
        }

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

    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException("Account no longer exists"));
        user.incrementTokenVersion();
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException("Account no longer exists"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.incrementTokenVersion();
        userRepository.save(user);
    }
}
