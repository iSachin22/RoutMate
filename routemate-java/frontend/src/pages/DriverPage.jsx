import { useCallback, useEffect, useState } from 'react';
import { Alert, Button, Card, Col, Container, Form, Row, Spinner } from 'react-bootstrap';
import { api, asArray, getSession } from '../api.js';
import { CITIES } from '../data/cities.js';
import NavBar from '../components/NavBar.jsx';
import WaypointEditor from '../components/WaypointEditor.jsx';
import TripCard from '../components/TripCard.jsx';
import BookingsModal from '../components/BookingsModal.jsx';

function defaultDepartureLocal() {
  const d = new Date(Date.now() + 2 * 3600 * 1000);
  d.setMinutes(Math.ceil(d.getMinutes() / 15) * 15, 0, 0);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** Driver dashboard: publish trips, manage the trip feed, verify OTPs. */
export default function DriverPage() {
  const session = getSession();
  const [waypoints, setWaypoints] = useState([0, 1]); // Pune → Mumbai
  const [form, setForm] = useState({
    availableSeats: 3,
    availableBootKg: 15,
    baseFare: 350,
    departureTime: defaultDepartureLocal(),
  });
  const [fieldErrors, setFieldErrors] = useState({});
  const [publishAlert, setPublishAlert] = useState(null);
  const [publishing, setPublishing] = useState(false);

  const [trips, setTrips] = useState([]);
  const [tripsLoading, setTripsLoading] = useState(true);
  const [tripsError, setTripsError] = useState('');

  const [modalTrip, setModalTrip] = useState(null);

  const loadTrips = useCallback(() => {
    setTripsLoading(true);
    setTripsError('');
    api.getDriverTrips(session.userId)
      .then((data) => setTrips(asArray(data, 'trips')))
      .catch((err) => setTripsError(err.userMessage || err.message))
      .finally(() => setTripsLoading(false));
  }, [session.userId]);

  useEffect(() => {
    loadTrips();
  }, [loadTrips]);

  const publish = async (e) => {
    e.preventDefault();
    setPublishAlert(null);
    const errors = {};
    for (let i = 1; i < waypoints.length; i += 1) {
      if (waypoints[i] === waypoints[i - 1]) errors.waypoints = 'Choose different cities for consecutive points.';
    }
    const seats = Number(form.availableSeats);
    const boot = Number(form.availableBootKg);
    const fare = Number(form.baseFare);
    if (!Number.isInteger(seats) || seats < 1) errors.seats = 'Offer at least 1 seat.';
    if (!Number.isFinite(boot) || boot < 0) errors.boot = 'Boot capacity must be 0 or more.';
    if (!Number.isFinite(fare) || fare <= 0) errors.fare = 'Base fare must be a positive amount.';
    if (!form.departureTime) errors.departure = 'Pick a departure date and time.';
    setFieldErrors(errors);
    if (Object.keys(errors).length) return;

    const chosen = waypoints.map((idx) => CITIES[idx]);
    const payload = {
      driverId: session.userId,
      originName: chosen[0].name,
      destinationName: chosen[chosen.length - 1].name,
      routeWaypoints: chosen.map((c) => ({ lng: c.lng, lat: c.lat })),
      availableSeats: seats,
      availableBootKg: boot,
      baseFare: fare,
      departureTime: new Date(form.departureTime).toISOString(),
    };

    setPublishing(true);
    try {
      const trip = await api.publishTrip(payload);
      setPublishAlert({
        type: 'success',
        message: `Trip published! Trip ID: ${trip?.tripId || 'created'}`,
      });
      setWaypoints([0, 1]);
      setForm({ availableSeats: 3, availableBootKg: 15, baseFare: 350, departureTime: defaultDepartureLocal() });
      loadTrips();
    } catch (err) {
      setPublishAlert({ type: 'danger', message: err.userMessage || err.message });
    } finally {
      setPublishing(false);
    }
  };

  return (
    <>
      <NavBar subtitle="Driver" />
      <Container className="rm-container">
        <Row className="g-4">
          <Col lg={5}>
            <Card className="rm-card p-4">
              <div className="rm-section-label">Publish</div>
              <h5 className="mb-1">Publish a new trip</h5>
              <p className="text-muted small mb-3">
                Pick cities from the dropdowns — coordinates are attached automatically.
              </p>
              {publishAlert && (
                <Alert variant={publishAlert.type} onClose={() => setPublishAlert(null)} dismissible>
                  {publishAlert.message}
                </Alert>
              )}
              <Form onSubmit={publish} noValidate>
                <Form.Group className="mb-3">
                  <Form.Label>Route waypoints</Form.Label>
                  <WaypointEditor value={waypoints} onChange={setWaypoints} />
                  {fieldErrors.waypoints && (
                    <div className="text-danger small mt-1">{fieldErrors.waypoints}</div>
                  )}
                </Form.Group>
                <Row className="g-3">
                  <Col xs={6}>
                    <Form.Group>
                      <Form.Label>Available seats</Form.Label>
                      <Form.Control
                        type="number" min={1} max={8} step={1}
                        value={form.availableSeats}
                        isInvalid={!!fieldErrors.seats}
                        onChange={(e) => setForm({ ...form, availableSeats: e.target.value })}
                      />
                      <Form.Control.Feedback type="invalid">{fieldErrors.seats}</Form.Control.Feedback>
                    </Form.Group>
                  </Col>
                  <Col xs={6}>
                    <Form.Group>
                      <Form.Label>Boot capacity (kg)</Form.Label>
                      <Form.Control
                        type="number" min={0} max={500} step={1}
                        value={form.availableBootKg}
                        isInvalid={!!fieldErrors.boot}
                        onChange={(e) => setForm({ ...form, availableBootKg: e.target.value })}
                      />
                      <Form.Control.Feedback type="invalid">{fieldErrors.boot}</Form.Control.Feedback>
                    </Form.Group>
                  </Col>
                  <Col xs={6}>
                    <Form.Group>
                      <Form.Label>Base fare (₹)</Form.Label>
                      <Form.Control
                        type="number" min={1} step={1}
                        value={form.baseFare}
                        isInvalid={!!fieldErrors.fare}
                        onChange={(e) => setForm({ ...form, baseFare: e.target.value })}
                      />
                      <Form.Control.Feedback type="invalid">{fieldErrors.fare}</Form.Control.Feedback>
                    </Form.Group>
                  </Col>
                  <Col xs={6}>
                    <Form.Group>
                      <Form.Label>Departure</Form.Label>
                      <Form.Control
                        type="datetime-local"
                        value={form.departureTime}
                        isInvalid={!!fieldErrors.departure}
                        onChange={(e) => setForm({ ...form, departureTime: e.target.value })}
                      />
                      <Form.Control.Feedback type="invalid">{fieldErrors.departure}</Form.Control.Feedback>
                    </Form.Group>
                  </Col>
                </Row>
                <Button className="btn-rm w-100 mt-4" type="submit" disabled={publishing}>
                  {publishing ? 'Publishing…' : 'Publish Trip'}
                </Button>
              </Form>
            </Card>
          </Col>

          <Col lg={7}>
            <Card className="rm-card p-4">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <div>
                  <div className="rm-section-label">Manage</div>
                  <h5 className="mb-0">Your trips</h5>
                </div>
                <Button variant="outline-secondary" size="sm" onClick={loadTrips}>↻ Refresh</Button>
              </div>
              {tripsLoading && (
                <div className="empty-state">
                  <Spinner animation="border" size="sm" className="me-2" /> Loading your trips…
                </div>
              )}
              {!tripsLoading && tripsError && <Alert variant="danger">{tripsError}</Alert>}
              {!tripsLoading && !tripsError && trips.length === 0 && (
                <div className="empty-state">No trips yet. Publish your first trip using the form.</div>
              )}
              {!tripsLoading &&
                trips.map((trip) => (
                  <TripCard key={trip.tripId} trip={trip} onViewBookings={() => setModalTrip(trip)} />
                ))}
            </Card>
          </Col>
        </Row>
        <p className="footer-note">RouteMate · stateless JWT auth · Spring Boot backend on http://localhost:8081</p>
      </Container>

      <BookingsModal show={!!modalTrip} onHide={() => setModalTrip(null)} trip={modalTrip} />
    </>
  );
}
