package com.computershare.regfiling.web;

import com.computershare.regfiling.domain.Filing;
import com.computershare.regfiling.domain.User;
import com.computershare.regfiling.domain.UserRole;
import com.computershare.regfiling.service.ApprovalService;
import com.computershare.regfiling.web.dto.ApiExceptions;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// openapi.yaml: POST /filings/{id}/approve — FR-011/FR-020.
@RestController
@RequestMapping("/api/filings")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @PostMapping("/{id}/approve")
    public Filing approve(@PathVariable("id") String filingId, HttpServletRequest request) {
        User currentUser = CurrentUserResolver.require(request);
        if (currentUser.getRole() != UserRole.LEGAL_COMPLIANCE) {
            throw new ApiExceptions.ForbiddenException("Only Legal & Compliance users may approve filings.");
        }
        return approvalService.approve(filingId, currentUser.getId());
    }
}
