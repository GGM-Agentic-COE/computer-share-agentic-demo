import type { Filing } from '../types';

// Shared full-field mock builder — used by DeadlineRiskBanner.test.tsx and FilingReview.test.tsx
// so both stay in sync with Filing's real shape instead of duplicating ~30 fields twice.
export function makeFiling(overrides: Partial<Filing> = {}): Filing {
  return {
    id: 'f1',
    formType: 'FORM_4',
    executiveId: 'exec-1',
    status: 'VALIDATED',
    issuer: 'Ascendion INC — Demo Issuer',
    issuerTicker: 'ASND',
    reportingPerson: 'J. Alvarez',
    reportingPersonLast: 'Alvarez',
    reportingPersonFirst: 'Jordan',
    reportingPersonMiddle: null,
    reportingPersonStreet: '482 Harborview Terrace',
    reportingPersonCity: 'Wilmington',
    reportingPersonState: 'DE',
    reportingPersonZip: '19801',
    relationshipDirector: true,
    relationshipOfficer: false,
    relationshipTenPercentOwner: false,
    relationshipOther: false,
    officerTitle: null,
    titleOfSecurity: 'Common Stock',
    transactionDate: '2026-08-30',
    transactionCode: 'S',
    acquiredOrDisposed: 'D',
    shares: 100,
    pricePerShare: 10,
    sharesOwnedFollowingTransaction: 12300,
    ownershipForm: 'D',
    signedBy: null,
    signedAt: null,
    validationErrors: [],
    deadlineAt: new Date().toISOString(),
    edgarCutoffAt: new Date().toISOString(),
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    ...overrides,
  };
}
