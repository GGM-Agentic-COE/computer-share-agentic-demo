package com.computershare.regfiling.web;

import com.computershare.regfiling.domain.Filing;
import com.computershare.regfiling.domain.User;
import com.computershare.regfiling.domain.UserRole;
import com.computershare.regfiling.service.FormGenerationService;
import com.computershare.regfiling.web.dto.ApiExceptions;
import com.computershare.regfiling.web.dto.SimulateTradeRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// openapi.yaml: POST /demo/simulate-trade — stands in for a real EquatePlus transaction event (FR-001).
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final FormGenerationService formGenerationService;

    public DemoController(FormGenerationService formGenerationService) {
        this.formGenerationService = formGenerationService;
    }

    @PostMapping("/simulate-trade")
    public ResponseEntity<Filing> simulateTrade(@Valid @RequestBody SimulateTradeRequest request,
                                                 HttpServletRequest servletRequest) {
        User currentUser = CurrentUserResolver.require(servletRequest);

        // An executive may only generate a filing attributed to themselves — the request body's
        // executiveId must match the authenticated caller. Found missing at the Phase 6
        // code-review gate: previously any authenticated user could attribute a trade to any
        // other executive by supplying their id in the request body.
        if (currentUser.getRole() != UserRole.EXECUTIVE || !currentUser.getId().equals(request.getExecutiveId())) {
            throw new ApiExceptions.ForbiddenException(
                    "You may only simulate a trade for your own executiveId.");
        }

        Filing filing = formGenerationService.generate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(filing);
    }
}
