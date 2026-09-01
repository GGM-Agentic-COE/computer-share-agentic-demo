import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../AuthContext';
import type { Filing, FilingStatus } from '../types';
import { FilingStatusBadge } from '../components/FilingStatusBadge';

// wireframes.md Screen 2 — Compliance Dashboard. WU-11. Sorted by deadline urgency by default
// per user-journeys.md's drop-off-risk design decision.
export function ComplianceDashboard() {
  const { authHeader } = useAuth();
  const [filings, setFilings] = useState<Filing[]>([]);
  const [status, setStatus] = useState<FilingStatus | ''>('');
  const [search, setSearch] = useState('');

  useEffect(() => {
    api
      .listFilings(authHeader, { status: status || undefined, search: search || undefined })
      .then((fs) => {
        const sorted = [...fs].sort(
          (a, b) => new Date(a.edgarCutoffAt).getTime() - new Date(b.edgarCutoffAt).getTime(),
        );
        setFilings(sorted);
      })
      .catch(() => undefined);
  }, [authHeader, status, search]);

  return (
    <div>
      <h2>Compliance Dashboard</h2>
      <div style={{ display: 'flex', gap: 10, marginBottom: 10 }}>
        <label>
          Status:{' '}
          <select
            aria-label="Filter by status"
            value={status}
            onChange={(e) => setStatus(e.target.value as FilingStatus | '')}
          >
            <option value="">All</option>
            <option value="INCOMPLETE">Incomplete</option>
            <option value="UNDER_REVIEW">Under Review</option>
            <option value="VALIDATED">Validated</option>
            <option value="SUBMITTED">Submitted</option>
          </select>
        </label>
        <input
          aria-label="Search"
          placeholder="Search issuer or executive…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr style={{ textAlign: 'left', borderBottom: '2px solid #ddd' }}>
            <th>Executive</th>
            <th>Form</th>
            <th>Status</th>
            <th>Deadline (EDGAR cutoff)</th>
          </tr>
        </thead>
        <tbody>
          {filings.map((f) => (
            <tr key={f.id} style={{ borderBottom: '1px solid #eee' }} data-testid="dashboard-row">
              <td>
                <Link to={`/filings/${f.id}`}>{f.executiveId}</Link>
              </td>
              <td>{f.formType}</td>
              <td>
                <FilingStatusBadge status={f.status} />
              </td>
              <td>{new Date(f.edgarCutoffAt).toLocaleString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {filings.length === 0 && <p>No filings match this filter.</p>}
    </div>
  );
}
