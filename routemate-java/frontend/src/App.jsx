import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { getSession } from './api.js';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import LoginPage from './pages/LoginPage.jsx';
import DriverPage from './pages/DriverPage.jsx';
import PassengerPage from './pages/PassengerPage.jsx';

function Home() {
  const session = getSession();
  if (!session) return <Navigate to="/login" replace />;
  return <Navigate to={session.role === 'DRIVER' ? '/driver' : '/passenger'} replace />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/driver"
          element={
            <ProtectedRoute role="DRIVER">
              <DriverPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/passenger"
          element={
            <ProtectedRoute role="PASSENGER">
              <PassengerPage />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<Home />} />
      </Routes>
    </BrowserRouter>
  );
}
