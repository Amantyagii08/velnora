
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

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "An account with this email already exists"
            );
        }

        User user = new User();

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPhone(request.getPhone());
        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );

        // Public registration defaults to CUSTOMER.
        // ADMIN cannot be selected by the user.
        user.setRole(UserRole.CUSTOMER);

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

    public AuthResponse login(LoginRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        request.getPassword()
                )
        );

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
