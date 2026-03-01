package com.example.demo.admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.admin.dto.DashboardStatsResponse;
import com.example.demo.admin.service.AdminDashboardService;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
	
	private final AdminDashboardService dashboardService;
	
	@Autowired
	public AdminDashboardController(AdminDashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}
	
	@GetMapping("/stats")
	public DashboardStatsResponse getStats() {
		return dashboardService.getStats();
	}
	
	
	
}
