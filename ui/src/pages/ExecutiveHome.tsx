import { useEffect, useState } from 'react';
import { api, ApiError } from '../api';
import { useAuth } from '../AuthContext';
import type { Filing } from '../types';
import { FilingStatusBadge } from '../components/FilingStatusBadge';
import { DeadlineRiskBanner } from '../components/DeadlineRiskBanner';

// wireframes.md Screen 1 — Executive Home. WU-10.
export function ExecutiveHome() {
  const { currentUser, authHeader } = useAuth();
  const [filings, setFilings] = useState<Filing[]>([]);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const refresh = () => {
    api.listFilings(authHeader, { executiveId: currentUser.id }).then(setFilings).catch(() => undefined);
  };

  useEffect(refresh, [authHeader, currentUser.id]);

  async function simulateTrade() {
    setBusy(true);
    setMessage(null);
    try {
      // Demo trade with valid, randomized-ish values so repeated clicks produce distinct filings.
      const shares = 100 + Math.floor(Math.random() * 900);
      await api.simulateTrade(authHeader, {
        executiveId: currentUser.id,
        transactionCode: 'S',
        shares,
        pricePerShare: Number((10 + Math.random() * 90).toFixed(2)),
      });
      setMessage('Form 4 generated for your trade.');
      refresh();
    } catch (err) {
      if (err instanceof ApiError) {
        setMessage(`Could not generate form (HTTP ${err.status}).`);
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <button onClick={simulateTrade} disabled={busy}>
        {busy ? 'Simulating…' : 'Simulate Trade'}
      </button>
      {message && <p role="status">{message}</p>}

      <h2>My Filings</h2>
      {filings.length === 0 && <p>No filings yet — simulate a trade to get started.</p>}
      <ul style={{ listStyle: 'none', padding: 0 }}>
        {filings.map((f) => (
          <li
            key={f.id}
            style={{ border: '1px solid #eee', borderRadius: 6, padding: 10, marginBottom: 8 }}
            data-testid="filing-row"
          >
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span>
                Form 4 · {f.transactionDate} · {f.shares} sh @ ${f.pricePerShare}
              </span>
              <FilingStatusBadge status={f.status} />
            </div>
            <DeadlineRiskBanner filing={f} />
          </li>
        ))}
      </ul>
    </div>
  );
}
