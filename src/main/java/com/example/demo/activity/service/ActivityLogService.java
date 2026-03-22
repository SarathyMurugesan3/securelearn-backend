package com.example.demo.activity.service;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.demo.activity.model.ActivityLog;
import com.example.demo.activity.repository.ActivityLogRepository;

/**
 * Central service for recording user activity events.
 *
 * Calls are @Async so logging never adds latency to the request thread.
 * Supported action constants (pass these as the `action` param):
 *   LOGIN, LOGOUT, PLAY_VIDEO, ACCESS_PDF, DOWNLOAD
 */
@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @Async
    public void logAction(String userId, String tenantId, String action, String ipAddress) {
        ActivityLog log = new ActivityLog(userId, tenantId, action, ipAddress);
        activityLogRepository.save(log);
        System.out.println("📋 ActivityLog [" + action + "] userId=" + userId + " ip=" + ipAddress);
    }

    public List<ActivityLog> getLogsForUser(String userId) {
        return activityLogRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    public List<ActivityLog> getLogsForTenant(String tenantId) {
        return activityLogRepository.findByTenantIdOrderByTimestampDesc(tenantId);
    }
}
