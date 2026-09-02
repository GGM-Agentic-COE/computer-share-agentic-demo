import type { AppNotification, AuditLogEntry, Filing, FilingStatus } from './types';

const BASE_URL = 'http://localhost:8080/api';

class ApiError extends Error {
  constructor(public status: number, public body: unknown) {
    super(`API error ${status}`);
  }
}

async function request<T>(path: string, authHeader: string, init: RequestInit = {}): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      Authorization: authHeader,
      ...init.headers,
    },
  });
  const isJson = res.headers.get('content-type')?.includes('application/json');
  const body = isJson ? await res.json() : undefined;
  if (!res.ok) {
    throw new ApiError(res.status, body);
  }
  return body as T;
}

export const api = {
  simulateTrade: (
    authHeader: string,
    payload: { executiveId: string; transactionCode?: string; shares?: number; pricePerShare?: number },
  ) => request<Filing>('/demo/simulate-trade', authHeader, { method: 'POST', body: JSON.stringify(payload) }),

  listFilings: (authHeader: string, params: { status?: FilingStatus; executiveId?: string; search?: string } = {}) => {
    const qs = new URLSearchParams(Object.entries(params).filter(([, v]) => v) as [string, string][]).toString();
    return request<Filing[]>(`/filings${qs ? `?${qs}` : ''}`, authHeader);
  },

  getFiling: (authHeader: string, id: string) => request<Filing>(`/filings/${id}`, authHeader),

  updateFiling: (authHeader: string, id: string, fields: Record<string, unknown>) =>
    request<Filing>(`/filings/${id}`, authHeader, { method: 'PATCH', body: JSON.stringify({ fields }) }),

  approveFiling: (authHeader: string, id: string) =>
    request<Filing>(`/filings/${id}/approve`, authHeader, { method: 'POST' }),

  auditLog: (authHeader: string, id: string) => request<AuditLogEntry[]>(`/filings/${id}/audit-log`, authHeader),

  // Binary response (application/pdf) — bypasses the generic request() helper, which always
  // parses JSON. A plain <a href> can't attach the Authorization header, so callers fetch this
  // Blob directly and trigger the download themselves via a temporary object URL.
  getFilingPdf: async (authHeader: string, id: string): Promise<Blob> => {
    const res = await fetch(`${BASE_URL}/filings/${id}/pdf`, { headers: { Authorization: authHeader } });
    if (!res.ok) {
      throw new ApiError(res.status, undefined);
    }
    return res.blob();
  },

  notifications: (authHeader: string, userId: string) =>
    request<AppNotification[]>(`/notifications?userId=${encodeURIComponent(userId)}`, authHeader),
};

export { ApiError };
