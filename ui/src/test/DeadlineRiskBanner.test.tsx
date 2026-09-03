import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { DeadlineRiskBanner } from '../components/DeadlineRiskBanner';
import { makeFiling } from './mockFiling';

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
