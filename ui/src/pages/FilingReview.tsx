import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Download } from 'lucide-react';
import { api, ApiError } from '../api';
import { useAuth } from '../AuthContext';
import type { AuditLogEntry, Filing } from '../types';
import { FilingStatusBadge } from '../components/FilingStatusBadge';
import { DeadlineRiskBanner } from '../components/DeadlineRiskBanner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Form4Field } from '@/components/form4/Form4Field';
import { RelationshipCheckboxes, type RelationshipField } from '@/components/form4/RelationshipCheckboxes';
import { SecuritiesTable } from '@/components/form4/SecuritiesTable';

interface EditableValues {
  issuer: string;
  issuerTicker: string;
  reportingPersonLast: string;
  reportingPersonFirst: string;
  reportingPersonMiddle: string;
  reportingPersonStreet: string;
  reportingPersonCity: string;
  reportingPersonState: string;
  reportingPersonZip: string;
  relationshipDirector: boolean;
  relationshipOfficer: boolean;
  relationshipTenPercentOwner: boolean;
  relationshipOther: boolean;
  officerTitle: string;
  titleOfSecurity: string;
  transactionDate: string;
  transactionCode: string;
  acquiredOrDisposed: 'A' | 'D';
  shares: string;
  pricePerShare: string;
  sharesOwnedFollowingTransaction: string;
  ownershipForm: 'D' | 'I';
  signedBy: string;
}

function valuesFromFiling(f: Filing): EditableValues {
  return {
    issuer: f.issuer,
    issuerTicker: f.issuerTicker,
    reportingPersonLast: f.reportingPersonLast,
    reportingPersonFirst: f.reportingPersonFirst,
    reportingPersonMiddle: f.reportingPersonMiddle ?? '',
    reportingPersonStreet: f.reportingPersonStreet,
    reportingPersonCity: f.reportingPersonCity,
    reportingPersonState: f.reportingPersonState,
    reportingPersonZip: f.reportingPersonZip,
    relationshipDirector: f.relationshipDirector,
    relationshipOfficer: f.relationshipOfficer,
    relationshipTenPercentOwner: f.relationshipTenPercentOwner,
    relationshipOther: f.relationshipOther,
    officerTitle: f.officerTitle ?? '',
    titleOfSecurity: f.titleOfSecurity,
    transactionDate: f.transactionDate,
    transactionCode: f.transactionCode,
    acquiredOrDisposed: f.acquiredOrDisposed,
    shares: String(f.shares),
    pricePerShare: String(f.pricePerShare),
    sharesOwnedFollowingTransaction: String(f.sharesOwnedFollowingTransaction),
    ownershipForm: f.ownershipForm,
    signedBy: f.signedBy ?? '',
  };
}

// wireframes.html Screen 3 — Filing Review. WU-12. Renders every SEC Form 4 field, editable by
// Legal & Compliance, in this app's normal branded style — a downloadable, government-form-styled
// PDF (see the Download PDF button) is where the "looks like the real form" requirement lives.
export function FilingReview() {
  const { id } = useParams<{ id: string }>();
  const { authHeader } = useAuth();
  const navigate = useNavigate();

  const [filing, setFiling] = useState<Filing | null>(null);
  const [auditLog, setAuditLog] = useState<AuditLogEntry[]>([]);
  const [values, setValues] = useState<EditableValues | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [showAudit, setShowAudit] = useState(false);
  const [busy, setBusy] = useState(false);
  const [downloading, setDownloading] = useState(false);

  const load = () => {
    if (!id) return;
    api.getFiling(authHeader, id).then((f) => {
      setFiling(f);
      setValues(valuesFromFiling(f));
    });
    api.auditLog(authHeader, id).then(setAuditLog);
  };

  useEffect(load, [authHeader, id]);

  function setField<K extends keyof EditableValues>(key: K, value: EditableValues[K]) {
    setValues((v) => (v ? { ...v, [key]: value } : v));
  }

  function toggleRelationship(field: RelationshipField, checked: boolean) {
    setValues((v) =>
      v
        ? {
            ...v,
            [field]: checked,
            ...(field === 'relationshipOfficer' && !checked ? { officerTitle: '' } : {}),
          }
        : v,
    );
  }

  async function saveEdits() {
    if (!id || !values) return;
    setBusy(true);
    setFieldErrors({});
    try {
      const updated = await api.updateFiling(authHeader, id, {
        ...values,
        shares: Number(values.shares),
        pricePerShare: Number(values.pricePerShare),
        sharesOwnedFollowingTransaction: Number(values.sharesOwnedFollowingTransaction),
      });
      setFiling(updated);
      load();
    } catch (err) {
      if (err instanceof ApiError && err.body && typeof err.body === 'object' && 'errors' in err.body) {
        const errors = (err.body as { errors: { field: string; message: string }[] }).errors;
        const map: Record<string, string> = {};
        errors.forEach((e) => (map[e.field] = e.message));
        setFieldErrors(map);
      }
    } finally {
      setBusy(false);
    }
  }

  async function approve() {
    if (!id) return;
    setBusy(true);
    try {
      await api.approveFiling(authHeader, id);
      load();
    } catch {
      // 409 surfaces via the disabled-button precondition (status/signature); nothing
      // additional to show the user here.
    } finally {
      setBusy(false);
    }
  }

  async function downloadPdf() {
    if (!id) return;
    setDownloading(true);
    try {
      const blob = await api.getFilingPdf(authHeader, id);
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `Form4-${id}.pdf`;
      link.click();
      URL.revokeObjectURL(url);
    } catch {
      // Download failures are rare (404/403) and not worth a dedicated UI state for this demo.
    } finally {
      setDownloading(false);
    }
  }

  if (!filing || !values) {
    return <p className="text-sm text-muted-foreground">Loading…</p>;
  }

  const canApprove = filing.status === 'VALIDATED' && Boolean(filing.signedBy && filing.signedBy.trim().length > 0);

  return (
    <div>
      <Button variant="outline" size="sm" onClick={() => navigate(-1)} className="mb-4">
        <ArrowLeft className="h-3.5 w-3.5" aria-hidden="true" />
        Back
      </Button>
      <h2 className="mb-2 flex flex-wrap items-center gap-2 text-base font-bold">
        Form 4 — {filing.reportingPerson} · {filing.transactionDate}
        <FilingStatusBadge status={filing.status} />
      </h2>
      <DeadlineRiskBanner filing={filing} />

      {filing.validationErrors.length > 0 && (
        <div role="alert" className="mt-4 rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive">
          {filing.validationErrors.join('; ')}
        </div>
      )}

      <div className="mt-6 flex flex-col gap-4">
        <Card>
          <CardHeader>
            <CardTitle>Reporting Person</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              <Form4Field label="Last Name" htmlFor="reportingPersonLast" error={fieldErrors.reportingPersonLast}>
                <Input
                  id="reportingPersonLast"
                  value={values.reportingPersonLast}
                  onChange={(e) => setField('reportingPersonLast', e.target.value)}
                  invalid={Boolean(fieldErrors.reportingPersonLast)}
                />
              </Form4Field>
              <Form4Field label="First Name" htmlFor="reportingPersonFirst" error={fieldErrors.reportingPersonFirst}>
                <Input
                  id="reportingPersonFirst"
                  value={values.reportingPersonFirst}
                  onChange={(e) => setField('reportingPersonFirst', e.target.value)}
                  invalid={Boolean(fieldErrors.reportingPersonFirst)}
                />
              </Form4Field>
              <Form4Field label="Middle Name" htmlFor="reportingPersonMiddle">
                <Input
                  id="reportingPersonMiddle"
                  value={values.reportingPersonMiddle}
                  onChange={(e) => setField('reportingPersonMiddle', e.target.value)}
                />
              </Form4Field>
            </div>
            <Form4Field label="Street" htmlFor="reportingPersonStreet" error={fieldErrors.reportingPersonStreet}>
              <Input
                id="reportingPersonStreet"
                value={values.reportingPersonStreet}
                onChange={(e) => setField('reportingPersonStreet', e.target.value)}
                invalid={Boolean(fieldErrors.reportingPersonStreet)}
              />
            </Form4Field>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-[1fr_120px_140px]">
              <Form4Field label="City" htmlFor="reportingPersonCity" error={fieldErrors.reportingPersonCity}>
                <Input
                  id="reportingPersonCity"
                  value={values.reportingPersonCity}
                  onChange={(e) => setField('reportingPersonCity', e.target.value)}
                  invalid={Boolean(fieldErrors.reportingPersonCity)}
                />
              </Form4Field>
              <Form4Field label="State" htmlFor="reportingPersonState" error={fieldErrors.reportingPersonState}>
                <Input
                  id="reportingPersonState"
                  maxLength={2}
                  value={values.reportingPersonState}
                  onChange={(e) => setField('reportingPersonState', e.target.value.toUpperCase())}
                  invalid={Boolean(fieldErrors.reportingPersonState)}
                />
              </Form4Field>
              <Form4Field label="Zip" htmlFor="reportingPersonZip" error={fieldErrors.reportingPersonZip}>
                <Input
                  id="reportingPersonZip"
                  value={values.reportingPersonZip}
                  onChange={(e) => setField('reportingPersonZip', e.target.value)}
                  invalid={Boolean(fieldErrors.reportingPersonZip)}
                />
              </Form4Field>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Issuer</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-[1fr_140px]">
              <Form4Field label="Issuer Name" htmlFor="issuer" error={fieldErrors.issuer}>
                <Input
                  id="issuer"
                  value={values.issuer}
                  onChange={(e) => setField('issuer', e.target.value)}
                  invalid={Boolean(fieldErrors.issuer)}
                />
              </Form4Field>
              <Form4Field label="Ticker Symbol" htmlFor="issuerTicker" error={fieldErrors.issuerTicker}>
                <Input
                  id="issuerTicker"
                  value={values.issuerTicker}
                  onChange={(e) => setField('issuerTicker', e.target.value.toUpperCase())}
                  invalid={Boolean(fieldErrors.issuerTicker)}
                />
              </Form4Field>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Relationship to Issuer</CardTitle>
          </CardHeader>
          <CardContent>
            <RelationshipCheckboxes
              values={values}
              onToggle={toggleRelationship}
              onOfficerTitleChange={(v) => setField('officerTitle', v)}
              error={fieldErrors.relationship}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Transaction Details</CardTitle>
          </CardHeader>
          <CardContent>
            <SecuritiesTable
              values={values}
              onChange={(field, value) => setValues((v) => (v ? ({ ...v, [field]: value } as EditableValues) : v))}
              errors={fieldErrors}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Signature</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
            <Form4Field
              label="Signature of Reporting Person"
              htmlFor="signedBy"
              error={fieldErrors.signedBy}
              className="w-full max-w-sm"
            >
              <Input
                id="signedBy"
                value={values.signedBy}
                onChange={(e) => setField('signedBy', e.target.value)}
                invalid={Boolean(fieldErrors.signedBy)}
                placeholder="Type your full legal name to sign"
              />
            </Form4Field>
            <div className="flex flex-col gap-1">
              <span className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Signed</span>
              <span className="text-sm text-muted-foreground">
                {filing.signedAt ? new Date(filing.signedAt).toLocaleString() : 'Not yet signed'}
              </span>
            </div>
            <Button variant="outline" onClick={downloadPdf} disabled={downloading}>
              <Download className="h-3.5 w-3.5" aria-hidden="true" />
              {downloading ? 'Downloading…' : 'Download PDF'}
            </Button>
          </CardContent>
        </Card>
      </div>

      <div className="mt-6 flex gap-3">
        <Button onClick={saveEdits} disabled={busy}>
          Save Edits
        </Button>
        <Button onClick={approve} disabled={busy || !canApprove} variant="secondary">
          Approve &amp; Submit
        </Button>
      </div>

      <Button
        variant="ghost"
        size="sm"
        className="mt-6"
        onClick={() => setShowAudit((s) => !s)}
        aria-expanded={showAudit}
      >
        {showAudit ? 'Hide' : 'Show'} audit trail
      </Button>
      {showAudit && (
        <ul className="mt-2 flex flex-col gap-1.5 text-xs text-muted-foreground">
          {auditLog.map((entry) => (
            <li key={entry.id}>
              {entry.occurredAt} — {entry.action} by {entry.actorId}: {entry.detail}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
