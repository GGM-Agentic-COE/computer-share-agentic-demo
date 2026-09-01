package com.computershare.regfiling.web;

import com.computershare.regfiling.domain.Notification;
import com.computershare.regfiling.domain.User;
import com.computershare.regfiling.service.NotificationService;
import com.computershare.regfiling.web.dto.ApiExceptions;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// openapi.yaml: GET /notifications — FR-006/FR-007/FR-008.
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<Notification> list(@RequestParam String userId, HttpServletRequest request) {
        User currentUser = CurrentUserResolver.require(request);

        // A user may only ever read their own notifications — the query param must match the
        // authenticated caller. Found missing (IDOR) at the Phase 6 code-review gate: previously
        // any authenticated user could read any other user's notifications by supplying their id.
        if (!currentUser.getId().equals(userId)) {
            throw new ApiExceptions.ForbiddenException("Users may only view their own notifications.");
        }

        return notificationService.forUser(userId);
    }
}
