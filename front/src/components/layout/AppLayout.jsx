import { Outlet } from 'react-router-dom';

import { getSession } from '../../services/marketplaceApi.js';
import { useAsync } from '../../hooks/useAsync.js';
import DashboardSidebar from './DashboardSidebar.jsx';
import Navbar from './Navbar.jsx';

export default function AppLayout() {
  const sessionState = useAsync(getSession, []);
  const session = sessionState.data ?? { authenticated: false, roles: [] };

  return (
    <div className="app-shell">
      <Navbar session={session} />
      <div className="app-body">
        <DashboardSidebar session={session} />
        <main className="app-content">
          <Outlet context={{ session }} />
        </main>
      </div>
    </div>
  );
}
