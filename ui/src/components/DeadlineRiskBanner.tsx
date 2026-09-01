import { TriangleAlert } from 'lucide-react';
import type { Filing } from '../types';
import { Alert, AlertDescription } from '@/components/ui/alert';

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
    <Alert variant="destructive" className="mt-1.5" aria-live="polite">
      <TriangleAlert className="h-4 w-4" aria-hidden="true" />
      <AlertDescription>
        Deadline risk: {Math.max(0, Math.round(hoursRemaining))}h remaining before the EDGAR same-day cutoff
      </AlertDescription>
    </Alert>
  );
}
