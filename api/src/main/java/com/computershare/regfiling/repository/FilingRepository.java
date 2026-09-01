package com.computershare.regfiling.repository;

import com.computershare.regfiling.domain.Filing;
import com.computershare.regfiling.domain.FilingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface FilingRepository extends JpaRepository<Filing, String> {

    List<Filing> findByExecutiveId(String executiveId);

    List<Filing> findByStatus(FilingStatus status);

    List<Filing> findByStatusNotAndDeadlineRiskFiredFalseAndEdgarCutoffAtBefore(
            FilingStatus excludedStatus, Instant threshold);
}
