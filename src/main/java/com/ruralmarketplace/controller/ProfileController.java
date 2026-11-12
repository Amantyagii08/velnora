package com.ruralmarketplace.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.ruralmarketplace.dto.UpdateProfileRequest;
import com.ruralmarketplace.entity.User;
import com.ruralmarketplace.entity.UserRole;
import com.ruralmarketplace.service.ProfileService;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService profileService;
    public ProfileController(ProfileService profileService) { this.profileService = profileService; }
    @GetMapping public Object get(@AuthenticationPrincipal Jwt jwt) {
        User user = profileService.get(jwt.getSubject());
        if (user.getRole() == UserRole.ADMIN) throw new IllegalArgumentException("Admin profile is not available here");
        return java.util.Map.of("id", user.getId(), "fullName", user.getFullName(), "email", user.getEmail(),
                "phone", user.getPhone() == null ? "" : user.getPhone(), "role", user.getRole().name());
    }
    @PutMapping public Object update(@Valid @RequestBody UpdateProfileRequest request, @AuthenticationPrincipal Jwt jwt) {
        User user = profileService.update(jwt.getSubject(), request);
        return java.util.Map.of("id", user.getId(), "fullName", user.getFullName(), "email", user.getEmail(),
                "phone", user.getPhone() == null ? "" : user.getPhone(), "role", user.getRole().name());
    }
}
