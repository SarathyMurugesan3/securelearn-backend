package com.example.demo.auth.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.auth.model.UserSession;
import com.example.demo.auth.repository.UserSessionRepository;
import com.example.demo.user.model.User;

@Service
public class SessionService {

    private final UserSessionRepository userSessionRepository;

    public SessionService(UserSessionRepository userSessionRepository) {
        this.userSessionRepository = userSessionRepository;
    }

    public UserSession createSession(User user, String ipAddress, String deviceInfo) {
        if ("STUDENT".equalsIgnoreCase(user.getRole())) {
            List<UserSession> activeSessions = userSessionRepository.findByUserIdAndIsActiveTrueOrderByLoginTimeAsc(user.getId());
            
            // If student already has 2 or more active sessions, invalidate the oldest one(s).
            if (activeSessions.size() >= 2) {
                int sessionsToInvalidate = activeSessions.size() - 1; // Leave exactly 1 so the new one makes 2
                for (int i = 0; i < sessionsToInvalidate; i++) {
                    UserSession oldestSession = activeSessions.get(i);
                    oldestSession.setActive(false);
                    userSessionRepository.save(oldestSession);
                }
            }
        }

        UserSession newSession = new UserSession(user.getId(), user.getTenantId(), ipAddress, deviceInfo);
        return userSessionRepository.save(newSession);
    }

    public boolean validateSession(String sessionId) {
        if (sessionId == null) return false;

        return userSessionRepository.findById(sessionId).map(session -> {
            if (session.isActive()) {
                session.setLastActive(LocalDateTime.now());
                userSessionRepository.save(session);
                return true;
            }
            return false;
        }).orElse(false);
    }

    public void invalidateSession(String sessionId) {
        userSessionRepository.findById(sessionId).ifPresent(session -> {
            session.setActive(false);
            userSessionRepository.save(session);
        });
    }
}
