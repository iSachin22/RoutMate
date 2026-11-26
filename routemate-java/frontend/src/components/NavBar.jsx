import { Navbar, Button, Container } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { getSession, logout } from '../api.js';

/** Shared top bar: brand, user chip, logout. */
export default function NavBar({ subtitle }) {
  const navigate = useNavigate();
  const session = getSession();

  return (
    <Navbar className="rm-navbar" expand="lg">
      <Container fluid className="rm-navbar-inner">
        <Navbar.Brand className="rm-brand">
          <svg width="32" height="32" viewBox="0 0 32 32" fill="none" aria-hidden="true">
            <rect width="32" height="32" rx="9" fill="rgba(255,255,255,.18)" />
            <path d="M8 20.5c0-1 .8-1.8 1.8-1.8h12.4c1 0 1.8.8 1.8 1.8v1.2c0 .6-.4 1-1 1h-1.3a1.7 1.7 0 0 1-3.4 0h-4.6a1.7 1.7 0 0 1-3.4 0H9c-.6 0-1-.4-1-1v-1.2Z" fill="#fff" />
            <path d="M10 18.7l1.6-4.4a1.6 1.6 0 0 1 1.5-1.1h5.8a1.6 1.6 0 0 1 1.5 1.1l1.6 4.4H10Z" fill="#fff" opacity=".85" />
          </svg>
          <span>
            RouteMate {subtitle && <small className="rm-nav-sub">· {subtitle}</small>}
          </span>
        </Navbar.Brand>
        <div className="rm-nav-right">
          <span className="user-chip d-none d-sm-inline-block">{session?.name || 'User'}</span>
          <Button
            className="btn-logout"
            onClick={() => {
              logout();
              navigate('/login', { replace: true });
            }}
          >
            Logout
          </Button>
        </div>
      </Container>
    </Navbar>
  );
}
