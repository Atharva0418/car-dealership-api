import type { ReactNode } from 'react';

import { Header } from './Header';

type AppLayoutProps = {
  currentView?: 'inventory' | 'purchases';
  children: ReactNode;
  onViewChange?: (view: 'inventory' | 'purchases') => void;
};

export function AppLayout({ children, currentView = 'inventory', onViewChange }: AppLayoutProps) {
  return (
    <div className="min-h-screen bg-slate-50 text-slate-950">
      <Header currentView={currentView} onViewChange={onViewChange} />
      <main className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">{children}</main>
    </div>
  );
}
