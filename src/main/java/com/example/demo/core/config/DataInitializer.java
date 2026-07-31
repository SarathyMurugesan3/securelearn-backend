package com.example.demo.core.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.demo.user.model.User;
import com.example.demo.user.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${super.admin.email:sarathyofficial90@gmail.com}")
	private String adminEmail;

	@Value("${super.admin.password:Sarathy@2006}")
	private String adminPassword;
	
	@Autowired
	public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}
	
	@Override
	public void run(String... args) {
		try {
			String primaryEmail = "sarathyofficial90@gmail.com";
			String primaryPassword = "Sarathy@2006";

			User admin = userRepository.findByEmail(primaryEmail).orElse(null);
			if (admin == null) {
				admin = new User("Super Admin", primaryEmail, passwordEncoder.encode(primaryPassword), "ADMIN", null, null);
				userRepository.save(admin);
				System.out.println("✅ Super Admin Created: " + primaryEmail);
			} else {
				admin.setPassword(passwordEncoder.encode(primaryPassword));
				admin.setBlocked(false);
				admin.setRiskScore(0);
				userRepository.save(admin);
				System.out.println("✅ Super Admin password force synced to Sarathy@2006 for: " + primaryEmail);
			}

			// Also ensure admin@securelearn.com / admin123 exists
			if (userRepository.findByEmail("admin@securelearn.com").isEmpty()) {
				User defaultAdmin = new User("Default Admin", "admin@securelearn.com", passwordEncoder.encode("admin123"), "ADMIN", null, null);
				userRepository.save(defaultAdmin);
			}
		} catch (Exception e) {
			System.out.println("⚠️ DataInitializer skipped — MongoDB not yet available: " + e.getMessage());
		}
	}
}
