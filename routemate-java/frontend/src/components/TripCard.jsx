import { Badge, Button, Card } from 'react-bootstrap';
import { formatDateTime, formatMoney } from '../api.js';

/** One trip in the driver's trip feed. */
export default function TripCard({ trip, onViewBookings }) {
  return (
    <Card className="trip-card mb-3">
      <Card.Body>
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
          <div>
            <h6 className="mb-1 fw-bold">
              {trip.originName} <span className="route-arrow">&#10230;</span> {trip.destinationName}
            </h6>
            <div className="text-muted small mb-1">Departure: {formatDateTime(trip.departureTime)}</div>
            <div className="small">
              <span className="badge-soft me-2">
                Seats left: <strong>{trip.availableSeats}</strong>
              </span>
              <span className="badge-soft me-2">
                Boot: <strong>{trip.availableBootKg} kg</strong>
              </span>
              <span className="badge-soft">
                Base fare: <strong>{formatMoney(trip.baseFare)}</strong>
              </span>
            </div>
          </div>
          <div className="text-end">
            <Badge bg="info" className="status-pill">
              {trip.status || 'SCHEDULED'}
            </Badge>
            {trip.tripId && (
              <div className="small text-muted mt-1">ID: {String(trip.tripId).slice(0, 8)}…</div>
            )}
            <Button variant="outline-primary" size="sm" className="mt-2" onClick={onViewBookings}>
              View Bookings &amp; Verify OTP
            </Button>
          </div>
        </div>
      </Card.Body>
    </Card>
  );
}
