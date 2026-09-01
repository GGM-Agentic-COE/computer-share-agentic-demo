package com.computershare.regfiling.service;

import com.computershare.regfiling.domain.Filing;
import com.computershare.regfiling.domain.FilingStatus;
import com.computershare.regfiling.domain.NotificationType;
import com.computershare.regfiling.repository.FilingRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * FR-008/E2.F2-S2. Fires DEADLINE_RISK against edgarCutoffAt (the 5:30pm ET EDGAR same-day
 * cutoff), NOT the raw statutory deadlineAt — see docs/sdlc/phase-0-vision/vision.md Regulatory
 * Posture #2. Idempotent via Filing.deadlineRiskFired.
 */
@Service
public class DeadlineRiskEvaluator {

    private static final long RISK_WINDOW_HOURS = 12;

    private final FilingRepository filingRepository;
    private final NotificationService notificationService;

    public DeadlineRiskEvaluator(FilingRepository filingRepository, NotificationService notificationService) {
        this.filingRepository = filingRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void evaluate() {
        Instant threshold = Instant.now().plusSeconds(RISK_WINDOW_HOURS * 3600);
        List<Filing> atRisk = filingRepository
                .findByStatusNotAndDeadlineRiskFiredFalseAndEdgarCutoffAtBefore(FilingStatus.SUBMITTED, threshold);

        for (Filing filing : atRisk) {
            notificationService.notifyUser(filing.getExecutiveId(), filing.getId(), NotificationType.DEADLINE_RISK,
                    "Deadline risk: your Form 4 filing is due soon and has not been submitted.");
            notificationService.notifyAllLegalCompliance(filing.getId(), NotificationType.DEADLINE_RISK,
                    "Deadline risk: filing " + filing.getId() + " has not been submitted.");
            filing.setDeadlineRiskFired(true);
            filingRepository.save(filing);
        }
    }
}
