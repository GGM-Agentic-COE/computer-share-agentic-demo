package com.computershare.regfiling;

// L1-construction-unit-test-generator · Phase 6 · Correlation ID C885C23C-949E-440C-8B0E-04FDD166A559
// Covers test-cases.feature scenarios S1, S2, S3, S4, S7, S8, S9, S12 end-to-end against a real
// (H2, in-process) Spring context — not mocked collaborators, so this exercises the actual wiring.

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FilingWorkflowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String EXEC_AUTH = "Bearer exec-1-token";
    private static final String LEGAL_AUTH = "Bearer legal-1-token";

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/filings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void s1_generateFormWithValidTrade_isImmediatelyValidated() throws Exception {
        mockMvc.perform(post("/api/demo/simulate-trade")
                        .header("Authorization", EXEC_AUTH)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "executiveId", "exec-1",
                                "transactionCode", "S",
                                "shares", 1200,
                                "pricePerShare", 42.10))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("VALIDATED"))
                .andExpect(jsonPath("$.issuer").exists())
                .andExpect(jsonPath("$.reportingPerson").value("J. Alvarez"));
    }

    @Test
    void s2_missingRequiredField_isIncompleteAndNotifiesBothPersonas() throws Exception {
        String body = mockMvc.perform(post("/api/demo/simulate-trade")
                        .header("Authorization", EXEC_AUTH)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "executiveId", "exec-1",
                                "shares", 100,
                                "pricePerShare", 10.0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("INCOMPLETE"))
                .andReturn().getResponse().getContentAsString();

        // executive notified
        mockMvc.perform(get("/api/notifications").param("userId", "exec-1").header("Authorization", EXEC_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type == 'INCOMPLETE_DATA')]").isNotEmpty());

        // legal notified (broadcast to all LEGAL_COMPLIANCE users)
        mockMvc.perform(get("/api/notifications").param("userId", "legal-1").header("Authorization", LEGAL_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type == 'INCOMPLETE_DATA')]").isNotEmpty());
    }

    @Test
    void s3_and_s7_invalidValueBlocksApproval_thenEditFixesAndApproveSucceeds() throws Exception {
        String response = mockMvc.perform(post("/api/demo/simulate-trade")
                        .header("Authorization", EXEC_AUTH)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "executiveId", "exec-1",
                                "transactionCode", "S",
                                "shares", 0,
                                "pricePerShare", 10.0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"))
                .andReturn().getResponse().getContentAsString();

        String filingId = objectMapper.readTree(response).get("id").asText();

        // S8: approval blocked pre-validation
        mockMvc.perform(post("/api/filings/{id}/approve", filingId).header("Authorization", LEGAL_AUTH))
                .andExpect(status().isConflict());

        // S5/S6: fix the invalid field via PATCH -> re-validated -> VALIDATED. signedBy rides
        // along in the same PATCH so no extra EDITED audit entry is introduced (keeps the
        // GENERATED/EDITED/APPROVED/SUBMITTED audit-index assertions below correct).
        mockMvc.perform(patch("/api/filings/{id}", filingId)
                        .header("Authorization", LEGAL_AUTH)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                Map.of("fields", Map.of("shares", 1200, "signedBy", "S. Kapoor")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDATED"));

        // S4/S7: approve now succeeds
        mockMvc.perform(post("/api/filings/{id}/approve", filingId).header("Authorization", LEGAL_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        // S9: audit trail has GENERATED, EDITED, APPROVED, SUBMITTED in order
        mockMvc.perform(get("/api/filings/{id}/audit-log", filingId).header("Authorization", LEGAL_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("GENERATED"))
                .andExpect(jsonPath("$[1].action").value("EDITED"))
                .andExpect(jsonPath("$[2].action").value("APPROVED"))
                .andExpect(jsonPath("$[3].action").value("SUBMITTED"));
    }

    @Test
    void executiveCannotApprove_rbacEnforced() throws Exception {
        String response = mockMvc.perform(post("/api/demo/simulate-trade")
                        .header("Authorization", EXEC_AUTH)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "executiveId", "exec-1",
                                "transactionCode", "S",
                                "shares", 100,
                                "pricePerShare", 10.0))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String filingId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(post("/api/filings/{id}/approve", filingId).header("Authorization", EXEC_AUTH))
                .andExpect(status().isForbidden());
    }

    @Test
    void s14_dashboardShowsAllFilingsForLegalUser() throws Exception {
        mockMvc.perform(post("/api/demo/simulate-trade")
                .header("Authorization", "Bearer exec-2-token")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(Map.of(
                        "executiveId", "exec-2", "transactionCode", "P", "shares", 50, "pricePerShare", 5.0))));

        mockMvc.perform(get("/api/filings").header("Authorization", LEGAL_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void executiveCannotSimulateTradeForAnotherExecutive() throws Exception {
        mockMvc.perform(post("/api/demo/simulate-trade")
                        .header("Authorization", EXEC_AUTH) // exec-1
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "executiveId", "exec-2", "transactionCode", "S", "shares", 10, "pricePerShare", 1.0))))
                .andExpect(status().isForbidden());
    }

    @Test
    void notificationsAreScopedToTheAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/notifications").param("userId", "exec-2").header("Authorization", EXEC_AUTH))
                .andExpect(status().isForbidden());
    }

    @Test
    void submittedFilingCannotBeEditedAgain() throws Exception {
        String response = mockMvc.perform(post("/api/demo/simulate-trade")
                        .header("Authorization", EXEC_AUTH)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "executiveId", "exec-1", "transactionCode", "S", "shares", 100, "pricePerShare", 10.0))))
                .andReturn().getResponse().getContentAsString();
        String filingId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(patch("/api/filings/{id}", filingId)
                        .header("Authorization", LEGAL_AUTH)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("fields", Map.of("signedBy", "S. Kapoor")))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/filings/{id}/approve", filingId).header("Authorization", LEGAL_AUTH))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/filings/{id}", filingId)
                        .header("Authorization", LEGAL_AUTH)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("fields", Map.of("shares", 999)))))
                .andExpect(status().isConflict());
    }

    @Test
    void approveWithoutSignatureIsRejected() throws Exception {
        String response = mockMvc.perform(post("/api/demo/simulate-trade")
                        .header("Authorization", EXEC_AUTH)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "executiveId", "exec-1", "transactionCode", "S", "shares", 100, "pricePerShare", 10.0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("VALIDATED"))
                .andReturn().getResponse().getContentAsString();
        String filingId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(post("/api/filings/{id}/approve", filingId).header("Authorization", LEGAL_AUTH))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Filing " + filingId + " has not been signed and cannot be approved."));
    }

    @Test
    void editingEveryFieldAtOnceSucceeds() throws Exception {
        // Mirrors exactly what the UI actually sends on every Save Edits — the full field set,
        // not a diff. Regression coverage for a real bug found via live browser testing: the
        // resulting "Fields updated: [...]" audit-log detail string (~24 field names) exceeded
        // AuditLogEntry.detail's original VARCHAR(255) column and threw a 500 (fixed by widening
        // that column to 2000 — see AuditLogEntry.java).
        String response = mockMvc.perform(post("/api/demo/simulate-trade")
                        .header("Authorization", EXEC_AUTH)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "executiveId", "exec-1", "transactionCode", "S", "shares", 100, "pricePerShare", 10.0))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String filingId = objectMapper.readTree(response).get("id").asText();

        Map<String, Object> allFields = Map.ofEntries(
                Map.entry("issuer", "Ascendion INC — Demo Issuer"),
                Map.entry("issuerTicker", "ASND"),
                Map.entry("reportingPersonLast", "Alvarez"),
                Map.entry("reportingPersonFirst", "Jordan"),
                Map.entry("reportingPersonMiddle", ""),
                Map.entry("reportingPersonStreet", "482 Harborview Terrace"),
                Map.entry("reportingPersonCity", "Wilmington"),
                Map.entry("reportingPersonState", "DE"),
                Map.entry("reportingPersonZip", "19801"),
                Map.entry("relationshipDirector", true),
                Map.entry("relationshipOfficer", false),
                Map.entry("relationshipTenPercentOwner", false),
                Map.entry("relationshipOther", false),
                Map.entry("officerTitle", ""),
                Map.entry("titleOfSecurity", "Common Stock"),
                Map.entry("transactionDate", "2026-09-02"),
                Map.entry("transactionCode", "S"),
                Map.entry("acquiredOrDisposed", "D"),
                Map.entry("shares", 100),
                Map.entry("pricePerShare", 10.0),
                Map.entry("sharesOwnedFollowingTransaction", 12300),
                Map.entry("ownershipForm", "D"),
                Map.entry("signedBy", "S. Kapoor"));

        mockMvc.perform(patch("/api/filings/{id}", filingId)
                        .header("Authorization", LEGAL_AUTH)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("fields", allFields))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDATED"))
                .andExpect(jsonPath("$.signedBy").value("S. Kapoor"));

        mockMvc.perform(post("/api/filings/{id}/approve", filingId).header("Authorization", LEGAL_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void downloadFilingPdf_returnsApplicationPdf() throws Exception {
        String response = mockMvc.perform(post("/api/demo/simulate-trade")
                        .header("Authorization", EXEC_AUTH)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "executiveId", "exec-1", "transactionCode", "S", "shares", 100, "pricePerShare", 10.0))))
                .andReturn().getResponse().getContentAsString();
        String filingId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/filings/{id}/pdf", filingId).header("Authorization", LEGAL_AUTH))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("Form4-" + filingId + ".pdf")))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertTrue(
                        result.getResponse().getContentAsByteArray().length > 0));
    }

    @Test
    void executiveCannotDownloadAnotherExecutivesPdf() throws Exception {
        String response = mockMvc.perform(post("/api/demo/simulate-trade")
                        .header("Authorization", EXEC_AUTH) // exec-1
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "executiveId", "exec-1", "transactionCode", "S", "shares", 100, "pricePerShare", 10.0))))
                .andReturn().getResponse().getContentAsString();
        String filingId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/filings/{id}/pdf", filingId).header("Authorization", "Bearer exec-2-token"))
                .andExpect(status().isForbidden());
    }
}
