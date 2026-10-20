import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth.jsx';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header className="navbar">
      <div className="navbar-inner">
        <Link to="/" className="brand">
          <span className="brand-dot" /> Uptime Monitor
        </Link>
        <nav className="nav-links">
          <NavLink to="/" end>Monitors</NavLink>
          <NavLink to="/incidents">Incidents</NavLink>
        </nav>
        <div className="nav-user">
          <span className="muted">{user?.name}</span>
          <button className="btn btn-ghost" onClick={handleLogout}>Logout</button>
        </div>
      </div>
    </header>
  );
}
