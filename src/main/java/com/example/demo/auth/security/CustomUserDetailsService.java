package com.example.demo.auth.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.demo.user.model.User;
import com.example.demo.user.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	@Value("${super.admin.email:superadmin}")
	private String superAdminEmail;

	@Autowired
	public CustomUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		// Synthetic user interception for Super Admin (avoids DB hit)
		if (superAdminEmail.equals(email) || "superadmin".equals(email)) {
			return org.springframework.security.core.userdetails.User.builder()
					.username(email)
					.password("") // Password is not evaluated here during JWT parsing
					.authorities("SUPER_ADMIN")
					.disabled(false)
					.build();
		}

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
		String roleName = user.getRole().toUpperCase().trim();

		return org.springframework.security.core.userdetails.User.builder()
				.username(user.getEmail())
				.password(user.getPassword())
				.authorities(roleName) // This sets the authority to "STUDENT", "ADMIN", etc.
				.disabled(user.isBlocked())
				.build();
	}
}
