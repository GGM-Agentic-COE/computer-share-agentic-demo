import { useEffect, useState } from 'react';
import { api, ApiError } from '../api';
import { useAuth } from '../AuthContext';
import type { Filing } from '../types';
import { FilingStatusBadge } from '../components/FilingStatusBadge';
import { DeadlineRiskBanner } from '../components/DeadlineRiskBanner';
import { Button } from '@/components/ui/button';

// wireframes.html Screen 1 — Executive Home. WU-10.
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
      <Button onClick={simulateTrade} disabled={busy}>
        {busy ? 'Executing…' : 'Execute Trade'}
      </Button>
      {message && (
        <p role="status" className="mt-2 text-sm text-muted-foreground">
          {message}
        </p>
      )}

      <h2 className="mb-3 mt-8 text-xs font-bold uppercase tracking-wide text-muted-foreground">My Filings</h2>
      {filings.length === 0 && (
        <p className="text-sm text-muted-foreground">No filings yet — execute a trade to get started.</p>
      )}
      <ul className="flex flex-col gap-2">
        {filings.map((f) => (
          <li key={f.id} className="rounded-2xl border border-border p-4" data-testid="filing-row">
            <div className="flex items-center justify-between gap-3">
              <span className="text-sm">
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
