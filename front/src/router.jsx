import { createBrowserRouter } from 'react-router-dom';

import AppLayout from './components/layout/AppLayout.jsx';
import AuctionsPage from './pages/AuctionsPage.jsx';
import DashboardPage from './pages/DashboardPage.jsx';
import LegacyStatusPage from './pages/LegacyStatusPage.jsx';
import ListingsPage from './pages/ListingsPage.jsx';
import NotFoundPage from './pages/NotFoundPage.jsx';
import StorePage from './pages/StorePage.jsx';

export const routes = [
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <DashboardPage /> },
      { path: 'auctions', element: <AuctionsPage /> },
      { path: 'listings', element: <ListingsPage /> },
      { path: 'store', element: <StorePage /> },
      { path: 'legacy', element: <LegacyStatusPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
];

export function createAppRouter() {
  return createBrowserRouter(routes);
}
