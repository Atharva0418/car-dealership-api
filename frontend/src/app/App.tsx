import { AuthPage } from '../features/auth/components/AuthPage';
import { useAuth } from '../features/auth/context/AuthContext';
import { InventoryDashboardPage } from '../features/inventory/pages/InventoryDashboardPage';
import { AppLayout } from '../shared/components/layout/AppLayout';

function App() {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <AuthPage />;
  }

  return (
    <AppLayout>
      <InventoryDashboardPage />
    </AppLayout>
  );
}

export default App;
