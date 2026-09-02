import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi, type Mock } from 'vitest';
import { FilingReview } from '../pages/FilingReview';
import { AuthProvider } from '../AuthContext';
import { api, ApiError } from '../api';
import { makeFiling } from './mockFiling';
import type { Filing } from '../types';

vi.mock('../api', async () => {
  const actual = await vi.importActual<typeof import('../api')>('../api');
  return {
    ...actual,
    api: {
      getFiling: vi.fn(),
      auditLog: vi.fn(),
      updateFiling: vi.fn(),
      approveFiling: vi.fn(),
      getFilingPdf: vi.fn(),
    },
  };
});

function renderFilingReview(filing: Filing) {
  (api.getFiling as Mock).mockResolvedValue(filing);
  (api.auditLog as Mock).mockResolvedValue([]);
  return render(
    <MemoryRouter initialEntries={[`/filings/${filing.id}`]}>
      <AuthProvider>
        <Routes>
          <Route path="/filings/:id" element={<FilingReview />} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe('FilingReview', () => {
  it('renders every Form 4 field pre-filled from the fetched filing', async () => {
    renderFilingReview(makeFiling());

    expect(await screen.findByLabelText('Last Name')).toHaveValue('Alvarez');
    expect(screen.getByLabelText('First Name')).toHaveValue('Jordan');
    expect(screen.getByLabelText('Street')).toHaveValue('482 Harborview Terrace');
    expect(screen.getByLabelText('Issuer Name')).toHaveValue('Ascendion INC — Demo Issuer');
    expect(screen.getByLabelText('Ticker Symbol')).toHaveValue('ASND');
    expect(screen.getByLabelText(/title of security/i)).toHaveValue('Common Stock');
    expect(screen.getByLabelText(/^shares$/i)).toHaveValue('100');
  });

  it('keeps Approve & Submit disabled unless the filing is VALIDATED and signed', async () => {
    renderFilingReview(makeFiling({ status: 'VALIDATED', signedBy: null }));
    expect(await screen.findByRole('button', { name: /approve/i })).toBeDisabled();
  });

  it('enables Approve & Submit once VALIDATED and signed', async () => {
    renderFilingReview(makeFiling({ status: 'VALIDATED', signedBy: 'S. Kapoor' }));
    expect(await screen.findByRole('button', { name: /approve/i })).toBeEnabled();
  });

  it('leaves Approve & Submit disabled when signed but not yet VALIDATED', async () => {
    renderFilingReview(makeFiling({ status: 'UNDER_REVIEW', signedBy: 'S. Kapoor' }));
    expect(await screen.findByRole('button', { name: /approve/i })).toBeDisabled();
  });

  it('allows multiple relationship checkboxes to be checked at once', async () => {
    renderFilingReview(makeFiling({ relationshipDirector: false, relationshipTenPercentOwner: false }));

    const director = await screen.findByRole('checkbox', { name: 'Director' });
    const tenPercentOwner = screen.getByRole('checkbox', { name: '10% Owner' });
    fireEvent.click(director);
    fireEvent.click(tenPercentOwner);

    expect(screen.getAllByRole('checkbox', { checked: true })).toHaveLength(2);
  });

  it('surfaces the backend field-error message after an invalid save', async () => {
    (api.updateFiling as Mock).mockRejectedValue(
      new ApiError(422, { filingId: 'f1', errors: [{ field: 'shares', message: 'shares: must be greater than 0' }] }),
    );
    renderFilingReview(makeFiling());

    const shares = await screen.findByLabelText(/^shares$/i);
    fireEvent.change(shares, { target: { value: '-5' } });
    fireEvent.click(screen.getByRole('button', { name: /save edits/i }));

    expect(await screen.findByText(/must be/i)).toBeVisible();
  });
});
