package com.example.demo.admin.service;

import org.springframework.stereotype.Service;

import com.example.demo.admin.dto.DashboardStatsResponse;
import com.example.demo.content.repository.ContentRepository;
import com.example.demo.monitoring.repository.ActivityLogRepository;
import com.example.demo.user.repository.UserRepository;

@Service
public class AdminDashboardService {
	
	private final UserRepository userRepository;
	private final ContentRepository contentRepository;
	private final ActivityLogRepository activityLogRepository;
	
	public AdminDashboardService(UserRepository userRepository,ContentRepository contentRepository,ActivityLogRepository activityLogRepository) {
		this.userRepository = userRepository;
		this.contentRepository = contentRepository;
		this.activityLogRepository = activityLogRepository;
	}
	
	public DashboardStatsResponse getStats() {
		long totalUsers = userRepository.count();
		long blockedUsers = userRepository.countByBlocked(true);
		long highRiskUsers = userRepository.countByRiskScoreGreaterThan(30);
		long totalContent = contentRepository.count();
		long screenshotAttempts = activityLogRepository.countByAction("SCREENSHOT_ATTEMPT");
		return new DashboardStatsResponse(totalUsers,blockedUsers,totalContent,screenshotAttempts,highRiskUsers);
	}
	
	
	
	
}
