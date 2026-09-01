import type { Filing } from '../types';

// Reads the hours remaining against edgarCutoffAt (the 5:30pm ET EDGAR cutoff) — NOT deadlineAt —
// per vision.md Regulatory Posture #2 / lld.md DeadlineRiskEvaluator.
export function DeadlineRiskBanner({ filing }: { filing: Filing }) {
  if (filing.status === 'SUBMITTED') {
    return null;
  }
  const hoursRemaining = (new Date(filing.edgarCutoffAt).getTime() - Date.now()) / 3600000;
  if (hoursRemaining > 12) {
    return null;
  }

  return (
    <div
      role="alert"
      aria-live="polite"
      style={{
        background: '#fef2f2',
        border: '1px solid #b91c1c',
        color: '#7f1d1d',
        borderRadius: 6,
        padding: '6px 10px',
        fontSize: 13,
        marginTop: 4,
      }}
    >
      ⚠ Deadline risk: {Math.max(0, Math.round(hoursRemaining))}h remaining before the EDGAR same-day cutoff
    </div>
  );
}
