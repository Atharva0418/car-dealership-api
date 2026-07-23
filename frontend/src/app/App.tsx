import { useState } from 'react';

import { AuthPage } from '../features/auth/components/AuthPage';
import { useAuth } from '../features/auth/context/AuthContext';
import { InventoryDashboardPage } from '../features/inventory/pages/InventoryDashboardPage';
import { MyPurchasesPage } from '../features/purchases/pages/MyPurchasesPage';
import { AppLayout } from '../shared/components/layout/AppLayout';

type AppView = 'inventory' | 'purchases';

function App() {
  const { isAdmin, isAuthenticated } = useAuth();
  const [currentView, setCurrentView] = useState<AppView>('inventory');

  if (!isAuthenticated) {
    return <AuthPage />;
  }

  const visibleView = isAdmin ? 'inventory' : currentView;

  return (
    <AppLayout currentView={visibleView} onViewChange={setCurrentView}>
      {visibleView === 'purchases' ? <MyPurchasesPage /> : <InventoryDashboardPage />}
    </AppLayout>
  );
}

export default App;
