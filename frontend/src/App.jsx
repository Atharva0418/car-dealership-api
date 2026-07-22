import { AppLayout } from './components/layout/AppLayout.jsx';
import { InventoryPlaceholderPage } from './pages/InventoryPlaceholderPage.jsx';
import { AuthPage } from './pages/AuthPage.tsx';
import { useAuth } from './auth/AuthContext';

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
