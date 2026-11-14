package com.ruralmarketplace.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.ruralmarketplace.entity.User;
import com.ruralmarketplace.entity.UserRole;
import com.ruralmarketplace.repository.UserRepository;

@Component
public class AdminAccountSetup implements ApplicationRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String name;
    private final String email;
    private final String phone;
    private final String password;

    public AdminAccountSetup(UserRepository userRepository, PasswordEncoder passwordEncoder,
            @Value("${app.admin.setup.name:}") String name,
            @Value("${app.admin.setup.email:}") String email,
            @Value("${app.admin.setup.phone:}") String phone,
            @Value("${app.admin.setup.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.findByRole(UserRole.ADMIN).size() > 0) return;
        if (name.isBlank() && email.isBlank() && password.isBlank()) return;
        if (name.isBlank() || email.isBlank() || password.length() < 12) {
            throw new IllegalStateException("Set app.admin.setup.name, app.admin.setup.email and a password of at least 12 characters to create the first admin");
        }
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException("Admin setup email already belongs to an existing account");
        }
        User admin = new User();
        admin.setFullName(name.trim());
        admin.setEmail(normalizedEmail);
        admin.setPhone(phone.isBlank() ? null : phone.trim());
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(UserRole.ADMIN);
        userRepository.save(admin);
    }
}
