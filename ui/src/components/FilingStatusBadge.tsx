import type { FilingStatus } from '../types';
import { Badge, type BadgeProps } from '@/components/ui/badge';

const LABELS: Record<FilingStatus, string> = {
  INCOMPLETE: 'Incomplete',
  UNDER_REVIEW: 'Under Review',
  VALIDATED: 'Validated',
  SUBMITTED: 'Submitted',
};

const VARIANTS: Record<FilingStatus, BadgeProps['variant']> = {
  INCOMPLETE: 'incomplete',
  UNDER_REVIEW: 'review',
  VALIDATED: 'validated',
  SUBMITTED: 'submitted',
};

export function FilingStatusBadge({ status }: { status: FilingStatus }) {
  return <Badge variant={VARIANTS[status]}>{LABELS[status]}</Badge>;
}
