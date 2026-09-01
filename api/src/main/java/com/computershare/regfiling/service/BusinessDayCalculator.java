package com.computershare.regfiling.service;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Adds N business days, skipping weekends only — US federal holidays are NOT accounted for.
 * Documented limitation, not a silent gap: a real deployment needs a holiday calendar to be
 * fully correct against the SEC's actual "2 business days" rule.
 */
@Component
public class BusinessDayCalculator {

    public static final ZoneId ET = ZoneId.of("America/New_York");

    public LocalDate addBusinessDays(LocalDate start, int businessDays) {
        LocalDate date = start;
        int added = 0;
        while (added < businessDays) {
            date = date.plusDays(1);
            if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return date;
    }

    /** Raw statutory deadline: end of the Nth business day after the transaction. */
    public ZonedDateTime statutoryDeadline(LocalDate transactionDate, int businessDays) {
        return addBusinessDays(transactionDate, businessDays).atTime(LocalTime.of(23, 59)).atZone(ET);
    }

    /**
     * Effective EDGAR same-day filing cutoff: 5:30pm ET on the Nth business day — stricter than
     * the raw statutory deadline (vision.md Regulatory Posture #2). Deadline-risk logic must use
     * THIS, not statutoryDeadline().
     */
    public ZonedDateTime edgarCutoff(LocalDate transactionDate, int businessDays) {
        return addBusinessDays(transactionDate, businessDays).atTime(LocalTime.of(17, 30)).atZone(ET);
    }
}
