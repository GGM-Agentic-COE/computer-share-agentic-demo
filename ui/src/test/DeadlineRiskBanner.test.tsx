import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { DeadlineRiskBanner } from '../components/DeadlineRiskBanner';
import type { Filing } from '../types';

// S12: deadline-risk alert fires when < 12h remain against edgarCutoffAt.
function makeFiling(overrides: Partial<Filing>): Filing {
  return {
    id: 'f1',
    formType: 'FORM_4',
    executiveId: 'exec-1',
    status: 'VALIDATED',
    issuer: 'Acme',
    reportingPerson: 'J. Alvarez',
    transactionDate: '2026-08-30',
    transactionCode: 'S',
    shares: 100,
    pricePerShare: 10,
    validationErrors: [],
    deadlineAt: new Date().toISOString(),
    edgarCutoffAt: new Date().toISOString(),
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    ...overrides,
  };
}

describe('DeadlineRiskBanner', () => {
  it('shows a warning when less than 12 hours remain', () => {
    const filing = makeFiling({ edgarCutoffAt: new Date(Date.now() + 9 * 3600_000).toISOString() });
    render(<DeadlineRiskBanner filing={filing} />);
    expect(screen.getByRole('alert')).toHaveTextContent(/deadline risk/i);
  });

  it('shows nothing when more than 12 hours remain', () => {
    const filing = makeFiling({ edgarCutoffAt: new Date(Date.now() + 48 * 3600_000).toISOString() });
    render(<DeadlineRiskBanner filing={filing} />);
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('shows nothing once the filing is SUBMITTED, regardless of time remaining', () => {
    const filing = makeFiling({
      status: 'SUBMITTED',
      edgarCutoffAt: new Date(Date.now() + 1 * 3600_000).toISOString(),
    });
    render(<DeadlineRiskBanner filing={filing} />);
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
