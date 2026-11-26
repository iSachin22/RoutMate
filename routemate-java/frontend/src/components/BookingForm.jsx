import { useMemo, useState } from 'react';
import { Alert, Button, Col, Form, Row } from 'react-bootstrap';
import { api, estimateFare, formatMoney } from '../api.js';
import { CITIES } from '../data/cities.js';

/**
 * SEAT / PARCEL booking form with a live fare estimate.
 * onConfirmed(booking) fires after the server returns the booking + OTP.
 */
export default function BookingForm({ trip, initialPickup, initialDrop, onConfirmed }) {
  const [bookingType, setBookingType] = useState('SEAT');
  const [pickup, setPickup] = useState(initialPickup ?? 0);
  const [drop, setDrop] = useState(initialDrop ?? 1);
  const [seats, setSeats] = useState(1);
  const [weight, setWeight] = useState(5);
  const [fieldErrors, setFieldErrors] = useState({});
  const [alert, setAlert] = useState(null);
  const [busy, setBusy] = useState(false);

  const estimate = useMemo(
    () =>
      estimateFare({
        bookingType,
        baseFare: trip?.baseFare,
        bookedSeats: Number(seats) || 1,
        parcelWeightKg: Number(weight) || 0,
      }),
    [bookingType, trip, seats, weight]
  );

  const submit = async (e) => {
    e.preventDefault();
    setAlert(null);
    const errors = {};
    if (CITIES[pickup].name === CITIES[drop].name) errors.drop = 'Pickup and drop must be different cities.';
    if (bookingType === 'SEAT') {
      const n = Number(seats);
      if (!Number.isInteger(n) || n < 1) errors.seats = 'Book at least 1 seat.';
      else if (trip.availableSeats !== undefined && n > trip.availableSeats)
        errors.seats = `Only ${trip.availableSeats} seat(s) left on this trip.`;
    } else {
      const w = Number(weight);
      if (!(w > 0)) errors.weight = 'Parcel weight must be greater than 0 kg.';
      else if (trip.availableBootKg !== undefined && w > trip.availableBootKg)
        errors.weight = `Only ${trip.availableBootKg} kg of boot space left.`;
    }
    setFieldErrors(errors);
    if (Object.keys(errors).length) return;

    const payload = {
      tripId: trip.tripId,
      passengerId: localStorage.getItem('userId'),
      bookedSeats: bookingType === 'SEAT' ? Number(seats) : 0,
      parcelWeightKg: bookingType === 'PARCEL' ? Number(weight) : 0,
      pickupLat: CITIES[pickup].lat,
      pickupLng: CITIES[pickup].lng,
      dropLat: CITIES[drop].lat,
      dropLng: CITIES[drop].lng,
      bookingType,
    };

    setBusy(true);
    try {
      const booking = await api.createBooking(payload);
      onConfirmed(booking, bookingType);
    } catch (err) {
      setAlert({ type: 'danger', message: err.userMessage || err.message });
    } finally {
      setBusy(false);
    }
  };

  return (
    <Form onSubmit={submit} noValidate>
      <div className="type-toggle" role="group" aria-label="Booking type">
        <button
          type="button"
          className={bookingType === 'SEAT' ? 'active' : ''}
          onClick={() => setBookingType('SEAT')}
        >
          Book a Seat<small>Travel with your luggage</small>
        </button>
        <button
          type="button"
          className={bookingType === 'PARCEL' ? 'active' : ''}
          onClick={() => setBookingType('PARCEL')}
        >
          Send a Parcel<small>Boot-space delivery</small>
        </button>
      </div>

      {alert && <Alert variant={alert.type}>{alert.message}</Alert>}

      <Row className="g-3">
        <Col md={6}>
          <Form.Group>
            <Form.Label>Pickup city</Form.Label>
            <Form.Select value={pickup} onChange={(e) => setPickup(Number(e.target.value))}>
              {CITIES.map((c, i) => (
                <option key={c.name} value={i}>{c.name}</option>
              ))}
            </Form.Select>
          </Form.Group>
        </Col>
        <Col md={6}>
          <Form.Group>
            <Form.Label>Drop city</Form.Label>
            <Form.Select
              value={drop}
              isInvalid={!!fieldErrors.drop}
              onChange={(e) => setDrop(Number(e.target.value))}
            >
              {CITIES.map((c, i) => (
                <option key={c.name} value={i}>{c.name}</option>
              ))}
            </Form.Select>
            <Form.Control.Feedback type="invalid">{fieldErrors.drop}</Form.Control.Feedback>
          </Form.Group>
        </Col>
        {bookingType === 'SEAT' ? (
          <Col md={6}>
            <Form.Group>
              <Form.Label>Seats to book</Form.Label>
              <Form.Control
                type="number"
                min={1}
                step={1}
                value={seats}
                isInvalid={!!fieldErrors.seats}
                onChange={(e) => setSeats(e.target.value)}
              />
              <Form.Control.Feedback type="invalid">{fieldErrors.seats}</Form.Control.Feedback>
            </Form.Group>
          </Col>
        ) : (
          <Col md={6}>
            <Form.Group>
              <Form.Label>Parcel weight (kg)</Form.Label>
              <Form.Control
                type="number"
                min={0.1}
                step={0.1}
                value={weight}
                isInvalid={!!fieldErrors.weight}
                onChange={(e) => setWeight(e.target.value)}
              />
              <Form.Control.Feedback type="invalid">{fieldErrors.weight}</Form.Control.Feedback>
            </Form.Group>
          </Col>
        )}
      </Row>

      <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mt-3">
        <div className="small">
          Estimated fare <strong className="estimate-amount">{formatMoney(estimate)}</strong>{' '}
          <span className="text-muted">(final fare is calculated by the server)</span>
        </div>
        <Button className="btn-rm" type="submit" disabled={busy}>
          {busy ? 'Booking…' : 'Confirm Booking'}
        </Button>
      </div>
    </Form>
  );
}
