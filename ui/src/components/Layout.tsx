import { Link } from 'react-router-dom';
import type { ReactNode } from 'react';
import { DEMO_USERS, useAuth } from '../AuthContext';
import { NotificationBell } from './NotificationBell';

export function Layout({ children }: { children: ReactNode }) {
  const { currentUser, setCurrentUserId } = useAuth();

  return (
    <div style={{ fontFamily: 'system-ui, sans-serif', maxWidth: 960, margin: '0 auto', padding: 16 }}>
      <header
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          borderBottom: '2px solid #111',
          paddingBottom: 10,
          marginBottom: 16,
        }}
      >
        <nav style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
          <strong>Regulatory Filing</strong>
          <Link to="/">Executive Home</Link>
          <Link to="/dashboard">Compliance Dashboard</Link>
        </nav>
        <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
          <label>
            Logged in as:{' '}
            <select
              aria-label="Switch demo user"
              value={currentUser.id}
              onChange={(e) => setCurrentUserId(e.target.value)}
            >
              {DEMO_USERS.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.displayName} ({u.role === 'EXECUTIVE' ? 'Executive' : 'Legal & Compliance'})
                </option>
              ))}
            </select>
          </label>
          <NotificationBell />
        </div>
      </header>
      <main>{children}</main>
    </div>
  );
}
