import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { FilingStatusBadge } from '../components/FilingStatusBadge';

describe('FilingStatusBadge', () => {
  it('renders a human-readable label for each status', () => {
    render(<FilingStatusBadge status="UNDER_REVIEW" />);
    expect(screen.getByText('Under Review')).toBeInTheDocument();
  });

  it('renders SUBMITTED distinctly from VALIDATED', () => {
    const { rerender } = render(<FilingStatusBadge status="VALIDATED" />);
    expect(screen.getByText('Validated')).toBeInTheDocument();
    rerender(<FilingStatusBadge status="SUBMITTED" />);
    expect(screen.getByText('Submitted')).toBeInTheDocument();
  });
});
