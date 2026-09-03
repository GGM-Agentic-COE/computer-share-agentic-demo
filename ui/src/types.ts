// L1-construction-ui-code-generator · Phase 6 · mirrors docs/sdlc/phase-4-design/openapi.yaml schemas

export type FilingStatus = 'INCOMPLETE' | 'UNDER_REVIEW' | 'VALIDATED' | 'SUBMITTED';

export interface Filing {
  id: string;
  formType: string;
  executiveId: string;
  status: FilingStatus;
  issuer: string;
  issuerTicker: string;
  reportingPerson: string;
  reportingPersonLast: string;
  reportingPersonFirst: string;
  reportingPersonMiddle: string | null;
  reportingPersonStreet: string;
  reportingPersonCity: string;
  reportingPersonState: string;
  reportingPersonZip: string;
  relationshipDirector: boolean;
  relationshipOfficer: boolean;
  relationshipTenPercentOwner: boolean;
  relationshipOther: boolean;
  officerTitle: string | null;
  titleOfSecurity: string;
  transactionDate: string;
  transactionCode: string;
  acquiredOrDisposed: 'A' | 'D';
  shares: number;
  pricePerShare: number;
  sharesOwnedFollowingTransaction: number;
  ownershipForm: 'D' | 'I';
  signedBy: string | null;
  signedAt: string | null;
  validationErrors: string[];
  deadlineAt: string;
  edgarCutoffAt: string;
  createdAt: string;
  updatedAt: string;
}

export type NotificationType = 'FORM_GENERATED' | 'DEADLINE_RISK' | 'SUBMITTED' | 'INCOMPLETE_DATA';

export interface AppNotification {
  id: string;
  userId: string;
  filingId: string;
  type: NotificationType;
  message: string;
  read: boolean;
  createdAt: string;
}

export interface AuditLogEntry {
  id: number;
  filingId: string;
  action: 'GENERATED' | 'EDITED' | 'APPROVED' | 'SUBMITTED' | 'FLAGGED_INCOMPLETE';
  actorId: string;
  detail: string;
  occurredAt: string;
}

export type UserRole = 'EXECUTIVE' | 'LEGAL_COMPLIANCE';

export interface DemoUser {
  id: string;
  displayName: string;
  role: UserRole;
}
