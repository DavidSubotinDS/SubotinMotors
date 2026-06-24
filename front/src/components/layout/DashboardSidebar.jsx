import { NavLink } from 'react-router-dom';
import { Car, Gauge, Gavel, LayoutDashboard, Package, ShieldCheck, UserRound } from 'lucide-react';

import { backendUrl } from '../../config.js';

const primaryItems = [
  { to: '/', label: 'Overview', icon: LayoutDashboard },
  { to: '/auctions', label: 'Auctions', icon: Gavel },
  { to: '/listings', label: 'Fixed price', icon: Car },
  { to: '/store', label: 'Parts store', icon: Package },
  { to: '/legacy', label: 'JSP status', icon: Gauge },
];

export default function DashboardSidebar({ session }) {
  const roles = new Set(session.roles ?? []);
  const userLinks = session.authenticated
    ? [
        { href: backendUrl('/user/my-profile'), label: 'My profile', icon: UserRound },
        { href: backendUrl('/user/followed-auctions'), label: 'Watchlist', icon: Gavel },
        { href: backendUrl('/orders'), label: 'Orders', icon: Package },
      ]
    : [];
  const adminLinks = roles.has('ROLE_ADMIN')
    ? [
        { href: backendUrl('/admin/dashboard'), label: 'Admin dashboard', icon: ShieldCheck },
        { href: backendUrl('/admin/store/orders'), label: 'Store orders', icon: Package },
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
            <a key={item.href} className="sidebar-link" href={item.href}>
              <item.icon aria-hidden="true" size={18} />
              {item.label}
            </a>
          ))}
        </SidebarGroup>
      )}

      {adminLinks.length > 0 && (
        <SidebarGroup title="Admin">
          {adminLinks.map((item) => (
            <a key={item.href} className="sidebar-link" href={item.href}>
              <item.icon aria-hidden="true" size={18} />
              {item.label}
            </a>
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
