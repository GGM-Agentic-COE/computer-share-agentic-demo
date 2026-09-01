import { useEffect, useState } from 'react';
import { api } from '../api';
import { useAuth } from '../AuthContext';
import type { AppNotification } from '../types';

export function NotificationBell() {
  const { currentUser, authHeader } = useAuth();
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    let cancelled = false;
    const load = () => {
      api.notifications(authHeader, currentUser.id).then((ns) => {
        if (!cancelled) setNotifications(ns);
      }).catch(() => undefined);
    };
    load();
    const interval = setInterval(load, 5000);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [authHeader, currentUser.id]);

  return (
    <div style={{ position: 'relative' }}>
      <button aria-label="Notifications" onClick={() => setOpen((o) => !o)}>
        🔔 {notifications.length}
      </button>
      {open && (
        <div
          role="dialog"
          aria-label="Notifications panel"
          style={{
            position: 'absolute',
            right: 0,
            top: '110%',
            width: 320,
            background: 'white',
            border: '1px solid #ddd',
            borderRadius: 8,
            padding: 10,
            zIndex: 10,
          }}
        >
          <strong>Notifications</strong>
          <button aria-label="Close notifications" onClick={() => setOpen(false)} style={{ float: 'right' }}>
            ×
          </button>
          <ul style={{ listStyle: 'none', padding: 0, marginTop: 8 }}>
            {notifications.length === 0 && <li>No notifications.</li>}
            {notifications.map((n) => (
              <li key={n.id} style={{ padding: '6px 0', borderTop: '1px solid #eee', fontSize: 13 }}>
                {n.message}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
