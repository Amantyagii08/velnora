package com.ruralmarketplace.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruralmarketplace.dto.UpdateProfileRequest;
import com.ruralmarketplace.entity.User;
import com.ruralmarketplace.repository.UserRepository;

@Service
public class ProfileService {
    private final UserRepository userRepository;
    public ProfileService(UserRepository userRepository) { this.userRepository = userRepository; }
    public User get(String email) { return userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found")); }
    @Transactional public User update(String email, UpdateProfileRequest request) {
        User user = get(email); user.setFullName(request.getFullName().trim()); user.setPhone(request.getPhone()); return userRepository.save(user);
    }
}
