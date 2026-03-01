package com.example.demo.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.auth.dto.LoginRequest;
import com.example.demo.auth.dto.LoginResponse;
import com.example.demo.auth.service.AuthService;
import com.example.demo.core.config.RegistrationProperties;
import com.example.demo.user.dto.CreateUserRequest;
import com.example.demo.user.model.User;
import com.example.demo.user.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	
	private final AuthService authService;
	private final UserService userService;
	private final RegistrationProperties registrationProperties;
	
	@Autowired
	public AuthController(AuthService authService,UserService userService,RegistrationProperties registrationProperties) {
		this.authService = authService;
		this.userService = userService;
		this.registrationProperties = registrationProperties;
	}
	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody CreateUserRequest request){
		if(!registrationProperties.isPublicEnabled()) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body("Public registraion disabled");
		}
		request.setRole("STUDENT");
		User user = userService.createUser(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(user);
	}
	
	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request,HttpServletRequest httpRequest) {
		return authService.login(request,httpRequest);
	}
	
	@PostMapping("/refresh")
	public LoginResponse refresh(@RequestParam String refreshToken) {
		return authService.refreshToken(refreshToken);
	}
	
	@PostMapping("/logout")
	public String logout() {
		return "Client should delete token";
	}
	
	
}
