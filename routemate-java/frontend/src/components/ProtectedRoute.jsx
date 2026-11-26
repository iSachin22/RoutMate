import { Navigate } from 'react-router-dom';
import { getSession } from '../api.js';

/** Route guard: requires an authenticated session with the given role. */
export default function ProtectedRoute({ role, children }) {
  const session = getSession();
  if (!session) return <Navigate to="/login" replace />;
  if (role && session.role !== role) {
    return <Navigate to={session.role === 'DRIVER' ? '/driver' : '/passenger'} replace />;
  }
  return children;
}
