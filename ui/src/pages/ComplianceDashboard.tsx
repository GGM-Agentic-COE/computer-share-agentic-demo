import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../AuthContext';
import type { Filing, FilingStatus } from '../types';
import { FilingStatusBadge } from '../components/FilingStatusBadge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Input } from '@/components/ui/input';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';

const STATUS_ALL = 'ALL'; // Radix Select.Item can't take value="" — sentinel for "no filter"

// wireframes.html Screen 2 — Compliance Dashboard. WU-11. Sorted by deadline urgency by default
// per user-flows.md's drop-off-risk design decision.
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
      <h2 className="mb-4 text-base font-bold">Compliance Dashboard</h2>
      <div className="mb-4 flex flex-wrap gap-3">
        <Select
          value={status || STATUS_ALL}
          onValueChange={(v) => setStatus(v === STATUS_ALL ? '' : (v as FilingStatus))}
        >
          <SelectTrigger aria-label="Filter by status" className="h-9 w-[180px] text-xs">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={STATUS_ALL}>Status: All</SelectItem>
            <SelectItem value="INCOMPLETE">Incomplete</SelectItem>
            <SelectItem value="UNDER_REVIEW">Under Review</SelectItem>
            <SelectItem value="VALIDATED">Validated</SelectItem>
            <SelectItem value="SUBMITTED">Submitted</SelectItem>
          </SelectContent>
        </Select>
        <Input
          aria-label="Search"
          placeholder="Search issuer or executive…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="h-9 w-64 text-xs"
        />
      </div>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Executive</TableHead>
            <TableHead>Form</TableHead>
            <TableHead>Status</TableHead>
            <TableHead>Deadline (EDGAR cutoff)</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {filings.map((f) => (
            <TableRow key={f.id} data-testid="dashboard-row">
              <TableCell>
                <Link to={`/filings/${f.id}`} className="font-medium text-primary hover:underline">
                  {f.executiveId}
                </Link>
              </TableCell>
              <TableCell>{f.formType}</TableCell>
              <TableCell>
                <FilingStatusBadge status={f.status} />
              </TableCell>
              <TableCell>{new Date(f.edgarCutoffAt).toLocaleString()}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
      {filings.length === 0 && <p className="mt-4 text-sm text-muted-foreground">No filings match this filter.</p>}
    </div>
  );
}
