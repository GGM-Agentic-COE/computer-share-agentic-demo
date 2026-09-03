package com.computershare.regfiling.web;

import com.computershare.regfiling.domain.Filing;
import com.computershare.regfiling.domain.FilingStatus;
import com.computershare.regfiling.domain.User;
import com.computershare.regfiling.domain.UserRole;
import com.computershare.regfiling.repository.FilingRepository;
import com.computershare.regfiling.service.FilingEditService;
import com.computershare.regfiling.service.FilingPdfService;
import com.computershare.regfiling.web.dto.ApiExceptions;
import com.computershare.regfiling.web.dto.PatchFilingRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

// openapi.yaml: GET /filings, GET /filings/{id}, PATCH /filings/{id}, GET /filings/{id}/pdf —
// FR-009/FR-010/FR-013/FR-015.
@RestController
@RequestMapping("/api/filings")
public class FilingController {

    private final FilingRepository filingRepository;
    private final FilingEditService filingEditService;
    private final FilingPdfService filingPdfService;

    public FilingController(FilingRepository filingRepository, FilingEditService filingEditService,
                             FilingPdfService filingPdfService) {
        this.filingRepository = filingRepository;
        this.filingEditService = filingEditService;
        this.filingPdfService = filingPdfService;
    }

    @GetMapping
    public List<Filing> list(@RequestParam(required = false) FilingStatus status,
                              @RequestParam(required = false) String executiveId,
                              @RequestParam(required = false) String search,
                              HttpServletRequest request) {
        User currentUser = CurrentUserResolver.require(request);

        List<Filing> filings;
        if (executiveId != null) {
            filings = filingRepository.findByExecutiveId(executiveId);
        } else if (status != null) {
            filings = filingRepository.findByStatus(status);
        } else {
            filings = filingRepository.findAll();
        }

        // Executives only ever see their own filings, regardless of query params (RBAC — FR-009/013).
        if (currentUser.getRole() == UserRole.EXECUTIVE) {
            filings = filings.stream().filter(f -> f.getExecutiveId().equals(currentUser.getId())).toList();
        }

        if (search != null && !search.isBlank()) {
            String needle = search.toLowerCase(Locale.ROOT);
            filings = filings.stream()
                    .filter(f -> f.getIssuer().toLowerCase(Locale.ROOT).contains(needle)
                            || f.getExecutiveId().toLowerCase(Locale.ROOT).contains(needle))
                    .toList();
        }

        return filings;
    }

    @GetMapping("/{id}")
    public Filing get(@PathVariable String id, HttpServletRequest request) {
        User currentUser = CurrentUserResolver.require(request);
        Filing filing = filingRepository.findById(id)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("No such filing: " + id));

        if (currentUser.getRole() == UserRole.EXECUTIVE && !filing.getExecutiveId().equals(currentUser.getId())) {
            throw new ApiExceptions.ForbiddenException("Executives may only view their own filings.");
        }
        return filing;
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String id, HttpServletRequest request) {
        User currentUser = CurrentUserResolver.require(request);
        Filing filing = filingRepository.findById(id)
                .orElseThrow(() -> new ApiExceptions.NotFoundException("No such filing: " + id));

        if (currentUser.getRole() == UserRole.EXECUTIVE && !filing.getExecutiveId().equals(currentUser.getId())) {
            throw new ApiExceptions.ForbiddenException("Executives may only download their own filings.");
        }

        byte[] pdf = filingPdfService.render(filing);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("Form4-" + id + ".pdf").build().toString())
                .body(pdf);
    }

    @PatchMapping("/{id}")
    public Filing update(@PathVariable String id, @RequestBody PatchFilingRequest body, HttpServletRequest request) {
        User currentUser = CurrentUserResolver.require(request);
        if (currentUser.getRole() != UserRole.LEGAL_COMPLIANCE) {
            throw new ApiExceptions.ForbiddenException("Only Legal & Compliance users may edit filings.");
        }
        return filingEditService.edit(id, body.getFields(), currentUser.getId());
    }
}
