package com.ruralmarketplace.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruralmarketplace.dto.UpdateProfileRequest;
import com.ruralmarketplace.entity.User;
import com.ruralmarketplace.repository.UserRepository;
import com.ruralmarketplace.exception.ResourceNotFoundException;

@Service
public class ProfileService {
    private final UserRepository userRepository;
    public ProfileService(UserRepository userRepository) { this.userRepository = userRepository; }
    public User get(String email) { return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found")); }
    @Transactional public User update(String email, UpdateProfileRequest request) {
        User user = get(email);
        String phone = request.getPhone() == null || request.getPhone().isBlank()
                ? null : request.getPhone().trim();
        if (phone != null && userRepository.existsByPhoneAndIdNot(phone, user.getId())) {
            throw new IllegalArgumentException("That phone number is already used by another account");
        }
        user.setFullName(request.getFullName().trim());
        user.setPhone(phone);
        return userRepository.save(user);
    }
}
