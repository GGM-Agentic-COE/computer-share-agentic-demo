import { NavLink } from 'react-router-dom';
import type { ReactNode } from 'react';
import { DEMO_USERS, useAuth } from '../AuthContext';
import { NotificationBell } from './NotificationBell';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { cn } from '@/lib/utils';

export function Layout({ children }: { children: ReactNode }) {
  const { currentUser, setCurrentUserId } = useAuth();

  return (
    <div className="mx-auto min-h-screen max-w-5xl bg-background px-6 pb-16">
      <header className="flex flex-wrap items-center justify-between gap-4 border-b-2 border-foreground py-4">
        <nav className="flex items-center gap-6">
          <span className="text-sm font-bold">Regulatory Filing</span>
          <NavLink
            to="/"
            end
            className={({ isActive }) =>
              cn(
                'text-xs font-semibold text-muted-foreground transition-colors hover:text-foreground',
                isActive && 'text-primary',
              )
            }
          >
            Executive Home
          </NavLink>
          <NavLink
            to="/dashboard"
            className={({ isActive }) =>
              cn(
                'text-xs font-semibold text-muted-foreground transition-colors hover:text-foreground',
                isActive && 'text-primary',
              )
            }
          >
            Compliance Dashboard
          </NavLink>
        </nav>
        <div className="flex items-center gap-3">
          <label className="flex items-center gap-2 text-xs text-muted-foreground">
            <span className="hidden sm:inline">Logged in as:</span>
            <Select value={currentUser.id} onValueChange={setCurrentUserId}>
              <SelectTrigger aria-label="Switch demo user" className="h-8 min-w-[220px] text-xs">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {DEMO_USERS.map((u) => (
                  <SelectItem key={u.id} value={u.id}>
                    {u.displayName} ({u.role === 'EXECUTIVE' ? 'Executive' : 'Legal & Compliance'})
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </label>
          <NotificationBell />
        </div>
      </header>
      <main className="py-6">{children}</main>
    </div>
  );
}
