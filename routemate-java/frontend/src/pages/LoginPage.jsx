import { useEffect, useState } from 'react';
import { Alert, Button, Card, Col, Container, Form, Row, Spinner } from 'react-bootstrap';
import { useNavigate } from 'react-router-dom';
import { api, getSession } from '../api.js';

/** Login / Signup with role selection; redirects by role after auth. */
export default function LoginPage() {
  const navigate = useNavigate();
  const [tab, setTab] = useState('login');
  const [alert, setAlert] = useState(null);
  const [busy, setBusy] = useState(false);

  const [login, setLogin] = useState({ email: '', password: '' });
  const [register, setRegister] = useState({
    name: '', email: '', password: '', confirm: '', role: 'PASSENGER',
  });
  const [errors, setErrors] = useState({});

  useEffect(() => {
    const session = getSession();
    if (session) navigate(session.role === 'DRIVER' ? '/driver' : '/passenger', { replace: true });
  }, [navigate]);

  const isEmail = (v) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v);

  const doLogin = async (e) => {
    e.preventDefault();
    setAlert(null);
    const errs = {};
    if (!isEmail(login.email)) errs.loginEmail = 'Enter a valid email address.';
    if (!login.password) errs.loginPassword = 'Password is required.';
    setErrors(errs);
    if (Object.keys(errs).length) return;

    setBusy(true);
    try {
      const data = await api.login({ email: login.email, password: login.password });
      navigate(data.role === 'DRIVER' ? '/driver' : '/passenger', { replace: true });
    } catch (err) {
      setAlert({ type: 'danger', message: err.userMessage || err.message });
    } finally {
      setBusy(false);
    }
  };

  const doRegister = async (e) => {
    e.preventDefault();
    setAlert(null);
    const errs = {};
    if (register.name.trim().length < 2) errs.regName = 'Please enter your full name.';
    if (!isEmail(register.email)) errs.regEmail = 'Enter a valid email address.';
    if (register.password.length < 6) errs.regPassword = 'Password must be at least 6 characters.';
    if (register.confirm !== register.password) errs.regConfirm = 'Passwords do not match.';
    setErrors(errs);
    if (Object.keys(errs).length) return;

    setBusy(true);
    try {
      await api.register({
        name: register.name.trim(),
        email: register.email,
        password: register.password,
        role: register.role,
      });
      const data = await api.login({ email: register.email, password: register.password });
      navigate(data.role === 'DRIVER' ? '/driver' : '/passenger', { replace: true });
    } catch (err) {
      if (err.kind === 'AUTH') {
        setAlert({ type: 'warning', message: 'Account created, but automatic sign-in failed. Please sign in manually.' });
        setTab('login');
        setLogin({ email: register.email, password: '' });
      } else {
        setAlert({ type: 'danger', message: err.userMessage || err.message });
      }
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth-wrap">
      <aside className="auth-hero">
        <div className="hero-badge">🚗 Carpooling &amp; Parcel Delivery</div>
        <h1>Share the route.<br />Split the cost.</h1>
        <p className="lead">
          RouteMate connects drivers with empty seats to passengers and parcels heading the
          same way — verified with OTP, priced live.
        </p>
        <ul className="hero-list">
          <li><span className="hero-dot">1</span><span>Publish your trip with one tap using preset city routes.</span></li>
          <li><span className="hero-dot">2</span><span>Search corridor rides and see live fare estimates before you book.</span></li>
          <li><span className="hero-dot">3</span><span>Settle securely with a 4-digit OTP at pickup.</span></li>
        </ul>
      </aside>

      <main className="auth-panel">
        <Container fluid>
          <Row className="justify-content-center">
            <Col>
              <Card className="auth-card">
                <div className="brand-chip mb-3">
                  <svg width="30" height="30" viewBox="0 0 32 32" fill="none" aria-hidden="true">
                    <rect width="32" height="32" rx="9" fill="url(#rg)" />
                    <path d="M8 20.5c0-1 .8-1.8 1.8-1.8h12.4c1 0 1.8.8 1.8 1.8v1.2c0 .6-.4 1-1 1h-1.3a1.7 1.7 0 0 1-3.4 0h-4.6a1.7 1.7 0 0 1-3.4 0H9c-.6 0-1-.4-1-1v-1.2Z" fill="#fff" />
                    <path d="M10 18.7l1.6-4.4a1.6 1.6 0 0 1 1.5-1.1h5.8a1.6 1.6 0 0 1 1.5 1.1l1.6 4.4H10Z" fill="#fff" opacity=".85" />
                    <defs>
                      <linearGradient id="rg" x1="0" y1="0" x2="32" y2="32">
                        <stop stopColor="#3d5afe" /><stop offset="1" stopColor="#00b0ff" />
                      </linearGradient>
                    </defs>
                  </svg>
                  RouteMate
                </div>
                <h2>Welcome</h2>
                <p className="auth-sub">Sign in to your account or create a new one in seconds.</p>

                {alert && <Alert variant={alert.type} onClose={() => setAlert(null)} dismissible>{alert.message}</Alert>}

                <div className="nav-auth" role="tablist">
                  <button type="button" className={tab === 'login' ? 'active' : ''} onClick={() => setTab('login')}>
                    Sign In
                  </button>
                  <button type="button" className={tab === 'register' ? 'active' : ''} onClick={() => setTab('register')}>
                    Create Account
                  </button>
                </div>

                {tab === 'login' ? (
                  <Form onSubmit={doLogin} noValidate>
                    <Form.Group className="mb-3">
                      <Form.Label>Email address</Form.Label>
                      <Form.Control
                        type="email"
                        placeholder="you@example.com"
                        value={login.email}
                        isInvalid={!!errors.loginEmail}
                        onChange={(e) => setLogin({ ...login, email: e.target.value })}
                      />
                      <Form.Control.Feedback type="invalid">{errors.loginEmail}</Form.Control.Feedback>
                    </Form.Group>
                    <Form.Group className="mb-4">
                      <Form.Label>Password</Form.Label>
                      <Form.Control
                        type="password"
                        placeholder="••••••••"
                        value={login.password}
                        isInvalid={!!errors.loginPassword}
                        onChange={(e) => setLogin({ ...login, password: e.target.value })}
                      />
                      <Form.Control.Feedback type="invalid">{errors.loginPassword}</Form.Control.Feedback>
                    </Form.Group>
                    <Button className="btn-rm w-100" type="submit" disabled={busy}>
                      {busy ? <Spinner animation="border" size="sm" /> : 'Sign In'}
                    </Button>
                  </Form>
                ) : (
                  <Form onSubmit={doRegister} noValidate>
                    <Form.Group className="mb-3">
                      <Form.Label>Full name</Form.Label>
                      <Form.Control
                        placeholder="e.g. Aisha Khan"
                        value={register.name}
                        isInvalid={!!errors.regName}
                        onChange={(e) => setRegister({ ...register, name: e.target.value })}
                      />
                      <Form.Control.Feedback type="invalid">{errors.regName}</Form.Control.Feedback>
                    </Form.Group>
                    <Form.Group className="mb-3">
                      <Form.Label>Email address</Form.Label>
                      <Form.Control
                        type="email"
                        placeholder="you@example.com"
                        value={register.email}
                        isInvalid={!!errors.regEmail}
                        onChange={(e) => setRegister({ ...register, email: e.target.value })}
                      />
                      <Form.Control.Feedback type="invalid">{errors.regEmail}</Form.Control.Feedback>
                    </Form.Group>
                    <Form.Group className="mb-3">
                      <Form.Label>Password</Form.Label>
                      <Form.Control
                        type="password"
                        placeholder="At least 6 characters"
                        value={register.password}
                        isInvalid={!!errors.regPassword}
                        onChange={(e) => setRegister({ ...register, password: e.target.value })}
                      />
                      <Form.Control.Feedback type="invalid">{errors.regPassword}</Form.Control.Feedback>
                    </Form.Group>
                    <Form.Group className="mb-3">
                      <Form.Label>Confirm password</Form.Label>
                      <Form.Control
                        type="password"
                        placeholder="Repeat your password"
                        value={register.confirm}
                        isInvalid={!!errors.regConfirm}
                        onChange={(e) => setRegister({ ...register, confirm: e.target.value })}
                      />
                      <Form.Control.Feedback type="invalid">{errors.regConfirm}</Form.Control.Feedback>
                    </Form.Group>
                    <Form.Group className="mb-4">
                      <Form.Label className="d-block">I am registering as a…</Form.Label>
                      {['PASSENGER', 'DRIVER'].map((r) => (
                        <label
                          key={r}
                          className={`role-card ${register.role === r ? 'role-card-selected' : ''}`}
                        >
                          <input
                            type="radio"
                            name="registerRole"
                            checked={register.role === r}
                            onChange={() => setRegister({ ...register, role: r })}
                          />
                          <span>
                            <strong>{r === 'PASSENGER' ? 'Passenger' : 'Driver'}</strong>
                            <small>
                              {r === 'PASSENGER'
                                ? 'Book seats or send parcels on shared routes.'
                                : 'Publish trips, manage bookings, verify OTPs.'}
                            </small>
                          </span>
                        </label>
                      ))}
                    </Form.Group>
                    <Button className="btn-rm w-100" type="submit" disabled={busy}>
                      {busy ? <Spinner animation="border" size="sm" /> : 'Create Account'}
                    </Button>
                  </Form>
                )}
              </Card>
            </Col>
          </Row>
        </Container>
      </main>
    </div>
  );
}
