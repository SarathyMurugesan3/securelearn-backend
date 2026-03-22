package com.example.demo.risk.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.auth.model.UserSession;
import com.example.demo.auth.repository.UserSessionRepository;
import com.example.demo.risk.model.RiskEvent;
import com.example.demo.risk.model.UserRisk;
import com.example.demo.risk.repository.RiskEventRepository;
import com.example.demo.risk.repository.UserRiskRepository;
import com.example.demo.user.model.User;
import com.example.demo.user.repository.UserRepository;

@Service
public class RiskEngineService {

    // Scoring constants
    private static final int NEW_IP_SCORE          = 30;
    private static final int VPN_SUSPECTED_SCORE   = 40;
    private static final int MULTIPLE_DEVICES_SCORE = 50;
    private static final int CONCURRENT_SESSIONS_SCORE = 70;
    private static final int BLOCK_THRESHOLD       = 100;

    private final UserRiskRepository userRiskRepository;
    private final RiskEventRepository riskEventRepository;
    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;

    public RiskEngineService(UserRiskRepository userRiskRepository,
                             RiskEventRepository riskEventRepository,
                             UserSessionRepository userSessionRepository,
                             UserRepository userRepository) {
        this.userRiskRepository = userRiskRepository;
        this.riskEventRepository = riskEventRepository;
        this.userSessionRepository = userSessionRepository;
        this.userRepository = userRepository;
    }

    /**
     * Main entry point: evaluate all risk signals for this login/request and
     * accumulate the score into the user's UserRisk record.
     */
    public UserRisk calculateRisk(User user, String ipAddress, String deviceInfo) {
        UserRisk risk = userRiskRepository.findByUserId(user.getId())
                .orElseGet(() -> new UserRisk(user.getId(), user.getTenantId()));


        // Rule 1: New IP login
        if (isNewIp(user, ipAddress)) {
            int added = NEW_IP_SCORE;
            risk.setRiskScore(risk.getRiskScore() + added);
            log(user, "NEW_IP", added, ipAddress, deviceInfo);
        }

        // Rule 2: VPN / proxy suspected (heuristic: loopback or known proxy header present)
        if (isVpnSuspected(ipAddress)) {
            int added = VPN_SUSPECTED_SCORE;
            risk.setRiskScore(risk.getRiskScore() + added);
            log(user, "VPN_SUSPECTED", added, ipAddress, deviceInfo);
        }

        // Rule 3: Multiple devices (>2 distinct fingerprints)
        if (user.getDevices() != null && user.getDevices().size() > 2) {
            int added = MULTIPLE_DEVICES_SCORE;
            risk.setRiskScore(risk.getRiskScore() + added);
            log(user, "MULTIPLE_DEVICES", added, ipAddress, deviceInfo);
        }

        // Rule 4: Concurrent sessions (more than 1 active session already)
        List<UserSession> activeSessions = userSessionRepository.findByUserIdAndIsActiveTrueOrderByLoginTimeAsc(user.getId());
        if (activeSessions.size() > 1) {
            int added = CONCURRENT_SESSIONS_SCORE;
            risk.setRiskScore(risk.getRiskScore() + added);
            log(user, "CONCURRENT_SESSIONS", added, ipAddress, deviceInfo);
        }

        userRiskRepository.save(risk);

        // Enforce: block user if threshold exceeded
        enforceRiskPolicy(user, risk);

        return risk;
    }

    /**
     * Check if this IP is new for the user (not seen before in their device list).
     */
    private boolean isNewIp(User user, String ipAddress) {
        if (ipAddress == null || user.getDevices() == null) return false;
        return user.getDevices().stream()
                .noneMatch(d -> ipAddress.equals(d.getIpAddress()));
    }

    /**
     * Heuristic VPN/proxy detection: private IP ranges, localhost, or known proxy headers.
     * In production, replace with a reputable IP intelligence API (e.g. ipapi.co, IPQualityScore).
     */
    private boolean isVpnSuspected(String ipAddress) {
        if (ipAddress == null) return false;
        // Flag private/loopback ranges that should never come from a real student
        return ipAddress.startsWith("10.")
                || ipAddress.startsWith("192.168.")
                || ipAddress.startsWith("172.16.")
                || ipAddress.equals("127.0.0.1")
                || ipAddress.equals("0:0:0:0:0:0:0:1");
    }

    /**
     * If riskScore > BLOCK_THRESHOLD → block the user entirely.
     * Sync block status onto the User record so downstream checks are immediate.
     */
    private void enforceRiskPolicy(User user, UserRisk risk) {
        if (risk.getRiskScore() > BLOCK_THRESHOLD) {
            user.setBlocked(true);
            userRepository.save(user);
            System.out.println("⚠️  User " + user.getEmail() + " BLOCKED — risk score " + risk.getRiskScore());
        }
    }

    /**
     * Drop an immutable audit event into the risk_events collection.
     */
    private void log(User user, String eventType, int scoreAdded, String ipAddress, String deviceInfo) {
        RiskEvent event = new RiskEvent(user.getId(), user.getTenantId(), eventType, scoreAdded, ipAddress, deviceInfo);
        riskEventRepository.save(event);
    }

    /**
     * Look up the current risk summary for a user (used by video controller guard).
     */
    public UserRisk getRisk(String userId) {
        return userRiskRepository.findByUserId(userId).orElse(null);
    }
}
