import { NavLink } from 'react-router-dom';
import {
  Bell,
  CalendarClock,
  Car,
  CreditCard,
  Gavel,
  LayoutDashboard,
  Package,
  ShieldCheck,
  ShoppingCart,
  UserRound,
} from 'lucide-react';

const primaryItems = [
  { to: '/', label: 'Overview', icon: LayoutDashboard },
  { to: '/auctions', label: 'Auctions', icon: Gavel },
  { to: '/listings', label: 'Fixed price', icon: Car },
  { to: '/parts', label: 'Parts store', icon: Package },
];

export default function DashboardSidebar({ session }) {
  const roles = new Set(session.roles ?? []);
  const userLinks = session.authenticated
    ? [
        { to: '/user/profile', label: 'My profile', icon: UserRound },
        { to: '/user/auctions', label: 'My auctions', icon: Gavel },
        { to: '/user/listings', label: 'My listings', icon: Car },
        { to: '/user/followed-auctions', label: 'Watchlist', icon: Bell },
        { to: '/user/appointments', label: 'Appointments', icon: CalendarClock },
        { to: '/user/bids', label: 'Bids', icon: CreditCard },
        { to: '/cart', label: 'Cart', icon: ShoppingCart },
        { to: '/orders', label: 'Orders', icon: Package },
      ]
    : [];
  const adminLinks = roles.has('ROLE_ADMIN')
    ? [
        { to: '/admin/users', label: 'Users', icon: ShieldCheck },
        { to: '/admin/cars', label: 'Auction admin', icon: Gavel },
        { to: '/admin/transactions', label: 'Transactions', icon: CreditCard },
        { to: '/admin/store/parts', label: 'Store parts', icon: Package },
        { to: '/admin/store/orders', label: 'Store orders', icon: ShoppingCart },
      ]
    : [];

  return (
    <aside className="sidebar" aria-label="Workspace navigation">
      <div className="sidebar-card">
        <div className="sidebar-user">
          <span className="avatar">{session.authenticated ? initials(session.displayName) : 'AA'}</span>
          <div>
            <strong>{session.authenticated ? session.displayName || session.username : 'Guest'}</strong>
            <small>{roles.has('ROLE_ADMIN') ? 'Admin workspace' : 'Marketplace workspace'}</small>
          </div>
        </div>
      </div>

      <SidebarGroup title="React">
        {primaryItems.map((item) => (
          <NavLink key={item.to} className="sidebar-link" to={item.to} end={item.to === '/'}>
            <item.icon aria-hidden="true" size={18} />
            {item.label}
          </NavLink>
        ))}
      </SidebarGroup>

      {userLinks.length > 0 && (
        <SidebarGroup title="Account">
          {userLinks.map((item) => (
            <NavLink key={item.to} className="sidebar-link" to={item.to}>
              <item.icon aria-hidden="true" size={18} />
              {item.label}
            </NavLink>
          ))}
        </SidebarGroup>
      )}

      {adminLinks.length > 0 && (
        <SidebarGroup title="Admin">
          {adminLinks.map((item) => (
            <NavLink key={item.to} className="sidebar-link" to={item.to}>
              <item.icon aria-hidden="true" size={18} />
              {item.label}
            </NavLink>
          ))}
        </SidebarGroup>
      )}
    </aside>
  );
}

function SidebarGroup({ title, children }) {
  return (
    <section className="sidebar-group">
      <h2>{title}</h2>
      {children}
    </section>
  );
}

function initials(value = '') {
  const parts = value.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) {
    return 'U';
  }
  return parts.slice(0, 2).map((part) => part[0]).join('').toUpperCase();
}
