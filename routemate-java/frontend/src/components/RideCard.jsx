import { Button, Card } from 'react-bootstrap';
import { formatDateTime, formatMoney } from '../api.js';

/** One search result the passenger can select. */
export default function RideCard({ trip, onSelect }) {
  return (
    <Card className="ride-card mb-3">
      <Card.Body>
        <div className="d-flex flex-wrap justify-content-between align-items-center gap-2">
          <div>
            <h6 className="mb-1 fw-bold">
              {trip.originName} <span className="route-arrow">&#10230;</span> {trip.destinationName}
            </h6>
            <div className="small text-muted mb-1">
              {trip.driverName && (
                <>
                  Driver: <strong>{trip.driverName}</strong> ·{' '}
                </>
              )}
              Departs: {formatDateTime(trip.departureTime)}
            </div>
            <div className="small">
              <span className="badge-soft me-2">
                Seats: <strong>{trip.availableSeats}</strong>
              </span>
              <span className="badge-soft me-2">
                Boot: <strong>{trip.availableBootKg} kg</strong>
              </span>
              <span className="badge-soft">
                Base fare: <strong>{formatMoney(trip.baseFare)}</strong>
              </span>
            </div>
          </div>
          <Button className="btn-rm btn-sm" onClick={onSelect}>Select ride</Button>
        </div>
      </Card.Body>
    </Card>
  );
}
