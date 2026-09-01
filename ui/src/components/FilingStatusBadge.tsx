import type { FilingStatus } from '../types';

const LABELS: Record<FilingStatus, string> = {
  INCOMPLETE: 'Incomplete',
  UNDER_REVIEW: 'Under Review',
  VALIDATED: 'Validated',
  SUBMITTED: 'Submitted',
};

const COLORS: Record<FilingStatus, string> = {
  INCOMPLETE: '#b91c1c',
  UNDER_REVIEW: '#b45309',
  VALIDATED: '#0f766e',
  SUBMITTED: '#166534',
};

export function FilingStatusBadge({ status }: { status: FilingStatus }) {
  return (
    <span
      style={{
        color: COLORS[status],
        border: `1px solid ${COLORS[status]}`,
        borderRadius: 12,
        padding: '2px 10px',
        fontSize: 12,
        fontWeight: 600,
      }}
    >
      {LABELS[status]}
    </span>
  );
}
