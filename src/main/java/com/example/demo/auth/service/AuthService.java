package com.example.demo.auth.service;



import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.activity.service.ActivityLogService;
import com.example.demo.auth.dto.LoginRequest;
import com.example.demo.auth.dto.LoginResponse;
import com.example.demo.auth.security.JwtService;
import com.example.demo.device.service.DeviceService;
import com.example.demo.risk.service.RiskEngineService;
import com.example.demo.user.model.User;
import com.example.demo.user.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthService {

	
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final UserRepository userRepository;
	private final DeviceService deviceService;
	private final PasswordEncoder passwordEncoder;
	private final SessionService sessionService;
	private final RiskEngineService riskEngineService;
	private final ActivityLogService activityLogService;
	
	public AuthService(AuthenticationManager authenticationManager, JwtService jwtService, UserRepository userRepository,
			DeviceService deviceService, PasswordEncoder passwordEncoder, SessionService sessionService,
			RiskEngineService riskEngineService, ActivityLogService activityLogService) {
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
		this.userRepository = userRepository;
		this.deviceService = deviceService;
		this.passwordEncoder = passwordEncoder;
		this.sessionService = sessionService;
		this.riskEngineService = riskEngineService;
		this.activityLogService = activityLogService;
	}
	
	public LoginResponse login(LoginRequest request,HttpServletRequest httpRequest) {
		try {
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(),request.getPassword()));
		}catch(AuthenticationException ex) {
			throw new RuntimeException("Invalid email or password");
		}
		User user = userRepository.findByEmail(request.getEmail()).orElseThrow(()-> new RuntimeException("User not found"));
		if(user.isBlocked() && !"ADMIN".equals(user.getRole())) {
			throw new RuntimeException("User is blocked due to risk score");
		}
		System.out.println("DEBUG USER: " + user);
		deviceService.trackDevice(user, request.getFingerprint(), httpRequest);
		
		String ipAddress = httpRequest.getRemoteAddr();
		String deviceInfo = httpRequest.getHeader("User-Agent");
		com.example.demo.auth.model.UserSession session = sessionService.createSession(user, ipAddress, deviceInfo);

		// Risk scoring — only for non-ADMIN users; admins are exempt from risk-based blocking
		if (!"ADMIN".equals(user.getRole())) {
			riskEngineService.calculateRisk(user, ipAddress, deviceInfo);
			// Re-fetch user in case block status was just set by risk engine
			user = userRepository.findByEmail(user.getEmail()).orElseThrow();
			if (user.isBlocked()) {
				sessionService.invalidateSession(session.getId());
				throw new RuntimeException("Account blocked due to high risk score");
			}
		}

		String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getRole(), user.getTenantId(), session.getId());
		String refreshToken = jwtService.generateRefreshToken(user.getEmail());
		System.out.println(passwordEncoder.matches("admin123", user.getPassword()));
		// Log successful login event (async — no added latency)
		activityLogService.logAction(user.getId(), user.getTenantId(), "LOGIN", ipAddress);

		return new LoginResponse(accessToken, refreshToken);
	}
	public LoginResponse refreshToken(String refreshToken) {
		String email = jwtService.extractEmail(refreshToken);
		if(!jwtService.isTokenValid(refreshToken, email)) {
			throw new RuntimeException("Invalid refresh token");
		}
		User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("user not found"));
		// Optional: We can simply copy the existing session from the refresh token if stored, or allow without one. Providing null for now or keeping default.
		String newAccessToken = jwtService.generateAccessToken(user.getEmail(), user.getRole(), user.getTenantId(), null);
		return new LoginResponse(newAccessToken,refreshToken);
	}
	
}
