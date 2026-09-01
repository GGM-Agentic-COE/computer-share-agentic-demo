import { createContext, useContext, useState, type ReactNode } from 'react';
import type { DemoUser } from './types';

// Mocked SSO — matches api/src/main/java/.../config/DataSeeder.java. Real corporate SSO
// integration is out of MVP scope; this is a "log in as" picker standing in for it.
export const DEMO_USERS: DemoUser[] = [
  { id: 'exec-1', displayName: 'J. Alvarez', role: 'EXECUTIVE' },
  { id: 'exec-2', displayName: 'R. Chen', role: 'EXECUTIVE' },
  { id: 'exec-3', displayName: 'M. Okafor', role: 'EXECUTIVE' },
  { id: 'legal-1', displayName: 'S. Kapoor', role: 'LEGAL_COMPLIANCE' },
  { id: 'legal-2', displayName: 'D. Whitfield', role: 'LEGAL_COMPLIANCE' },
];

interface AuthContextValue {
  currentUser: DemoUser;
  setCurrentUserId: (id: string) => void;
  authHeader: string;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [currentUserId, setCurrentUserId] = useState(DEMO_USERS[0].id);
  const currentUser = DEMO_USERS.find((u) => u.id === currentUserId) ?? DEMO_USERS[0];
  const authHeader = `Bearer ${currentUser.id}-token`;

  return (
    <AuthContext.Provider value={{ currentUser, setCurrentUserId, authHeader }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return ctx;
}
