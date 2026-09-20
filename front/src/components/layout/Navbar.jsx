import { NavLink } from 'react-router-dom';
import { useState } from 'react';
import { LogIn, LogOut } from 'lucide-react';

import { authApi } from '../../services/reactApi.js';
import Button from '../ui/Button.jsx';

export default function Navbar({ session }) {
  const [error, setError] = useState('');
  const [signingOut, setSigningOut] = useState(false);
  async function logout() {
    setSigningOut(true);
    setError('');
    try {
      await authApi.logout();
      window.location.href = '/';
    } catch (failure) {
      setError(failure.message);
      setSigningOut(false);
    }
  }

  return (
    <header className="navbar">
      <NavLink className="brand" to="/">
        <span className="brand-mark">AA</span>
        <span>
          <strong>Autostrada Auctions</strong>
          <small>Marketplace</small>
        </span>
      </NavLink>

      <nav className="navbar-links" aria-label="Primary navigation">
        <NavLink to="/auctions">Auctions</NavLink>
        <NavLink to="/listings">Listings</NavLink>
        <NavLink to="/parts">Store</NavLink>
      </nav>

      <div className="navbar-actions">
        {session.authenticated ? (
          <>
            <span className="session-pill">{session.displayName || session.username}</span>
            <Button onClick={logout} disabled={signingOut} icon={LogOut} variant="ghost">
              Logout
            </Button>
          </>
        ) : (
          <Button href="/login" icon={LogIn}>
            Login
          </Button>
        )}
      </div>
      {error && <p role="alert">{error}</p>}
    </header>
  );
}
