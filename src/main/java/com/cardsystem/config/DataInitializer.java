package com.cardsystem.config;

import com.cardsystem.models.User;
import com.cardsystem.models.constants.UserRole;
import com.cardsystem.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner createDefaultAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                                @org.springframework.beans.factory.annotation.Value("${create.default.admin:false}") boolean createDefaultAdmin) {
        return args -> {
            if (!createDefaultAdmin) return;
            if (userRepository.count() == 0) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPasswordHash(passwordEncoder.encode("admin"));
                admin.setRole(UserRole.SUPER_ADMIN);
                admin.setFullName("Default Super Admin");
                userRepository.save(admin);
                System.out.println("[DataInitializer] created default super admin 'admin'/'admin'");
            }
        };
    }
}
