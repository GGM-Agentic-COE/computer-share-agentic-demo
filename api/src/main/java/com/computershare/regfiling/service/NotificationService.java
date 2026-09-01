package com.computershare.regfiling.service;

import com.computershare.regfiling.domain.Notification;
import com.computershare.regfiling.domain.NotificationType;
import com.computershare.regfiling.domain.User;
import com.computershare.regfiling.domain.UserRole;
import com.computershare.regfiling.repository.NotificationRepository;
import com.computershare.regfiling.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

// In-app channel only in MVP — email/SMS out of scope (features.json FEAT-6 note).
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public void notifyUser(String userId, String filingId, NotificationType type, String message) {
        notificationRepository.save(new Notification(userId, filingId, type, message));
    }

    // Broadcasts to all LEGAL_COMPLIANCE-role users — MVP has no per-filing officer assignment
    // (see docs/sdlc/phase-4-design/user-journeys.md, revised at the Phase 4 design-quality gate).
    public void notifyAllLegalCompliance(String filingId, NotificationType type, String message) {
        for (User user : userRepository.findAll()) {
            if (user.getRole() == UserRole.LEGAL_COMPLIANCE) {
                notifyUser(user.getId(), filingId, type, message);
            }
        }
    }

    public List<Notification> forUser(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
