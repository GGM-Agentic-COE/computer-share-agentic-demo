import type { ReactNode } from 'react';
import { Label } from '@/components/ui/label';

interface Form4FieldProps {
  label: string;
  htmlFor: string;
  error?: string | null;
  className?: string;
  children: ReactNode;
}

// Generic label + control + error wrapper, used for every Form 4 field on the review screen —
// avoids repeating this layout ~20 times across FilingReview.tsx.
export function Form4Field({ label, htmlFor, error, className, children }: Form4FieldProps) {
  return (
    <div className={className}>
      <Label htmlFor={htmlFor}>{label}</Label>
      <div className="mt-1">{children}</div>
      {error && <p className="mt-1 text-xs text-destructive">{error}</p>}
    </div>
  );
}
