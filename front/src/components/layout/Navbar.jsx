import { NavLink } from 'react-router-dom';
import { LogIn, LogOut } from 'lucide-react';

import { backendUrl } from '../../config.js';
import Button from '../ui/Button.jsx';

export default function Navbar({ session }) {
  return (
    <header className="navbar">
      <NavLink className="brand" to="/">
        <span className="brand-mark">AA</span>
        <span>
          <strong>Autostrada Auctions</strong>
          <small>React workspace</small>
        </span>
      </NavLink>

      <nav className="navbar-links" aria-label="Primary navigation">
        <NavLink to="/auctions">Auctions</NavLink>
        <NavLink to="/listings">Listings</NavLink>
        <NavLink to="/store">Store</NavLink>
      </nav>

      <div className="navbar-actions">
        {session.authenticated ? (
          <>
            <span className="session-pill">{session.displayName || session.username}</span>
            <Button href={backendUrl('/logout')} icon={LogOut} variant="ghost">
              Logout
            </Button>
          </>
        ) : (
          <Button href={backendUrl('/login')} icon={LogIn}>
            Login
          </Button>
        )}
      </div>
    </header>
  );
}
