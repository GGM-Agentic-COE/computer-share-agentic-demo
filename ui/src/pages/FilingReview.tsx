import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api, ApiError } from '../api';
import { useAuth } from '../AuthContext';
import type { AuditLogEntry, Filing } from '../types';
import { FilingStatusBadge } from '../components/FilingStatusBadge';

// wireframes.md Screen 3 — Filing Review. WU-12.
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
    return <p>Loading…</p>;
  }

  return (
    <div>
      <button onClick={() => navigate(-1)}>← Back</button>
      <h2>
        Form 4 — {filing.executiveId} · {filing.transactionDate} <FilingStatusBadge status={filing.status} />
      </h2>

      <dl>
        <dt>Issuer</dt>
        <dd>{filing.issuer}</dd>
        <dt>Reporting Person</dt>
        <dd>{filing.reportingPerson}</dd>
        <dt>Transaction Code</dt>
        <dd>{filing.transactionCode}</dd>

        <dt>
          <label htmlFor="shares">Shares</label>
        </dt>
        <dd>
          <input id="shares" aria-label="Shares" value={shares} onChange={(e) => setShares(e.target.value)} />
          {fieldErrors.shares && <div style={{ color: '#b91c1c' }}>{fieldErrors.shares}</div>}
        </dd>

        <dt>
          <label htmlFor="pricePerShare">Price</label>
        </dt>
        <dd>
          <input
            id="pricePerShare"
            aria-label="Price per share"
            value={pricePerShare}
            onChange={(e) => setPricePerShare(e.target.value)}
          />
          {fieldErrors.pricePerShare && <div style={{ color: '#b91c1c' }}>{fieldErrors.pricePerShare}</div>}
        </dd>
      </dl>

      {filing.validationErrors.length > 0 && (
        <div role="alert" style={{ color: '#b91c1c', marginBottom: 8 }}>
          {filing.validationErrors.join('; ')}
        </div>
      )}

      <div style={{ display: 'flex', gap: 10 }}>
        <button onClick={saveEdits} disabled={busy}>
          Save Edits
        </button>
        <button onClick={approve} disabled={busy || filing.status !== 'VALIDATED'}>
          Approve & Submit
        </button>
      </div>

      <button style={{ marginTop: 16 }} onClick={() => setShowAudit((s) => !s)}>
        {showAudit ? 'Hide' : 'Show'} audit trail
      </button>
      {showAudit && (
        <ul>
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
