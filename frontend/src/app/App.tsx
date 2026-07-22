import { AuthPage } from '../features/auth/components/AuthPage';
import { useAuth } from '../features/auth/context/AuthContext';
import { InventoryPlaceholderPage } from '../features/inventory/pages/InventoryPlaceholderPage';
import { AppLayout } from '../shared/components/layout/AppLayout';

function App() {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <AuthPage />;
  }

  return (
    <AppLayout>
      <InventoryPlaceholderPage />
    </AppLayout>
  );
}

export default App;
