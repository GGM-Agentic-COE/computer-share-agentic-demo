import { useEffect, useState } from 'react';
import { Bell } from 'lucide-react';
import { api } from '../api';
import { useAuth } from '../AuthContext';
import type { AppNotification } from '../types';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';

const TYPE_LABELS: Record<AppNotification['type'], string> = {
  FORM_GENERATED: 'Form Generated',
  DEADLINE_RISK: 'Deadline Risk',
  SUBMITTED: 'Submitted',
  INCOMPLETE_DATA: 'Incomplete Data',
};

export function NotificationBell() {
  const { currentUser, authHeader } = useAuth();
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    let cancelled = false;
    const load = () => {
      api
        .notifications(authHeader, currentUser.id)
        .then((ns) => {
          if (!cancelled) setNotifications(ns);
        })
        .catch(() => undefined);
    };
    load();
    const interval = setInterval(load, 5000);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [authHeader, currentUser.id]);

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <button
          aria-label={`Notifications (${notifications.length})`}
          className="inline-flex items-center gap-1.5 rounded-full border border-primary bg-primary/10 px-3 py-1 text-xs font-bold text-primary transition-colors hover:bg-primary/15 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          <Bell className="h-3.5 w-3.5" aria-hidden="true" />
          <span>{notifications.length}</span>
        </button>
      </PopoverTrigger>
      <PopoverContent>
        <div className="mb-2 flex items-center justify-between">
          <span className="text-sm font-bold">Notifications</span>
          <button
            aria-label="Close notifications"
            onClick={() => setOpen(false)}
            className="text-muted-foreground hover:text-foreground"
          >
            &times;
          </button>
        </div>
        <ul className="flex flex-col">
          {notifications.length === 0 && <li className="py-2 text-sm text-muted-foreground">No notifications.</li>}
          {notifications.map((n) => (
            <li key={n.id} className="border-t border-border py-2 first:border-t-0">
              <span className="mb-0.5 block text-[10px] font-bold uppercase tracking-wide text-secondary">
                {TYPE_LABELS[n.type]}
              </span>
              <span className="text-sm">{n.message}</span>
            </li>
          ))}
        </ul>
      </PopoverContent>
    </Popover>
  );
}
