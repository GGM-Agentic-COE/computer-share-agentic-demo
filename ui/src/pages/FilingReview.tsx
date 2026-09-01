import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { api, ApiError } from '../api';
import { useAuth } from '../AuthContext';
import type { AuditLogEntry, Filing } from '../types';
import { FilingStatusBadge } from '../components/FilingStatusBadge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

// wireframes.html Screen 3 — Filing Review. WU-12.
export function FilingReview() {
  const { id } = useParams<{ id: string }>();
  const { authHeader } = useAuth();
  const navigate = useNavigate();

  const [filing, setFiling] = useState<Filing | null>(null);
  const [auditLog, setAuditLog] = useState<AuditLogEntry[]>([]);
  const [shares, setShares] = useState('');
  const [pricePerShare, setPricePerShare] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [showAudit, setShowAudit] = useState(false);
  const [busy, setBusy] = useState(false);

  const load = () => {
    if (!id) return;
    api.getFiling(authHeader, id).then((f) => {
      setFiling(f);
      setShares(String(f.shares));
      setPricePerShare(String(f.pricePerShare));
    });
    api.auditLog(authHeader, id).then(setAuditLog);
  };

  useEffect(load, [authHeader, id]);

  async function saveEdits() {
    if (!id) return;
    setBusy(true);
    setFieldErrors({});
    try {
      const updated = await api.updateFiling(authHeader, id, {
        shares: Number(shares),
        pricePerShare: Number(pricePerShare),
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
      // 409 surfaces via the disabled-button precondition (status !== VALIDATED); nothing
      // additional to show the user here.
    } finally {
      setBusy(false);
    }
  }

  if (!filing) {
    return <p className="text-sm text-muted-foreground">Loading…</p>;
  }

  return (
    <div>
      <Button variant="outline" size="sm" onClick={() => navigate(-1)} className="mb-4">
        <ArrowLeft className="h-3.5 w-3.5" aria-hidden="true" />
        Back
      </Button>
      <h2 className="mb-6 flex flex-wrap items-center gap-2 text-base font-bold">
        Form 4 — {filing.executiveId} · {filing.transactionDate}
        <FilingStatusBadge status={filing.status} />
      </h2>

      <dl className="grid grid-cols-[160px_1fr] gap-x-4 gap-y-3">
        <dt className="pt-2 text-xs font-semibold text-muted-foreground">Issuer</dt>
        <dd className="pt-2 text-sm text-muted-foreground">{filing.issuer}</dd>
        <dt className="pt-2 text-xs font-semibold text-muted-foreground">Reporting Person</dt>
        <dd className="pt-2 text-sm text-muted-foreground">{filing.reportingPerson}</dd>
        <dt className="pt-2 text-xs font-semibold text-muted-foreground">Transaction Code</dt>
        <dd className="pt-2 text-sm text-muted-foreground">{filing.transactionCode}</dd>

        <dt>
          <Label htmlFor="shares">Shares</Label>
        </dt>
        <dd>
          <Input
            id="shares"
            aria-label="Shares"
            value={shares}
            onChange={(e) => setShares(e.target.value)}
            invalid={Boolean(fieldErrors.shares)}
            className="max-w-[220px]"
          />
          {fieldErrors.shares && <p className="mt-1 text-xs text-destructive">{fieldErrors.shares}</p>}
        </dd>

        <dt>
          <Label htmlFor="pricePerShare">Price per share</Label>
        </dt>
        <dd>
          <Input
            id="pricePerShare"
            aria-label="Price per share"
            value={pricePerShare}
            onChange={(e) => setPricePerShare(e.target.value)}
            invalid={Boolean(fieldErrors.pricePerShare)}
            className="max-w-[220px]"
          />
          {fieldErrors.pricePerShare && <p className="mt-1 text-xs text-destructive">{fieldErrors.pricePerShare}</p>}
        </dd>
      </dl>

      {filing.validationErrors.length > 0 && (
        <div role="alert" className="mt-4 rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive">
          {filing.validationErrors.join('; ')}
        </div>
      )}

      <div className="mt-6 flex gap-3">
        <Button onClick={saveEdits} disabled={busy}>
          Save Edits
        </Button>
        <Button onClick={approve} disabled={busy || filing.status !== 'VALIDATED'} variant="secondary">
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
