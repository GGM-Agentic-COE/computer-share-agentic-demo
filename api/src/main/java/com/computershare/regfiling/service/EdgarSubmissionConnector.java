package com.computershare.regfiling.service;

import com.computershare.regfiling.domain.Filing;
import org.springframework.stereotype.Service;

/**
 * MOCKED — does not call any real external SEC system. Directly reflects the still-open FR-020
 * regulatory dependency (docs/sdlc/phase-0-vision/vision.md Regulatory Posture #1): whether this
 * module files to EDGAR directly or hands off to an existing manual filing-agent process is
 * undetermined pending Legal & Compliance confirmation. The interface below is real and is the
 * seam a real implementation would replace; the body is a stand-in.
 */
@Service
public class EdgarSubmissionConnector {

    public SubmissionResult submit(Filing filing) {
        // Simulated confirmation — a real implementation calls SEC EDGAR (Regulation S-T) or an
        // agent-assisted filing service here.
        String confirmationId = "MOCK-EDGAR-" + filing.getId().substring(0, 8).toUpperCase();
        return new SubmissionResult(confirmationId);
    }

    public record SubmissionResult(String confirmationId) {
    }
}
