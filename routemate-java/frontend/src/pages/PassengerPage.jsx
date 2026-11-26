import { useState } from 'react';
import { Alert, Button, Card, Col, Container, Form, Row, Spinner } from 'react-bootstrap';
import { api, asArray, formatDateTime, formatMoney } from '../api.js';
import { CITIES } from '../data/cities.js';
import NavBar from '../components/NavBar.jsx';
import RideCard from '../components/RideCard.jsx';
import BookingForm from '../components/BookingForm.jsx';
import FareOtpCard from '../components/FareOtpCard.jsx';

/** Passenger dashboard: corridor search → select ride → book (SEAT/PARCEL) → fare + OTP. */
export default function PassengerPage() {
  const [pickup, setPickup] = useState(0); // Pune
  const [drop, setDrop] = useState(1); // Mumbai
  const [searchError, setSearchError] = useState('');
  const [searching, setSearching] = useState(false);
  const [results, setResults] = useState([]);
  const [searched, setSearched] = useState(false);

  const [selectedTrip, setSelectedTrip] = useState(null);
  const [result, setResult] = useState(null); // { booking, bookingType }
  const [bookingAlert, setBookingAlert] = useState(null);

  const search = async (e) => {
    e.preventDefault();
    setSearchError('');
    setBookingAlert(null);
    if (CITIES[pickup].name === CITIES[drop].name) {
      setSearchError('Pickup and drop must be different cities.');
      return;
    }
    setSearching(true);
    setSearched(true);
    setSelectedTrip(null);
    setResult(null);
    try {
      const data = await api.searchTrips({
        pickupLat: CITIES[pickup].lat,
        pickupLng: CITIES[pickup].lng,
        dropLat: CITIES[drop].lat,
        dropLng: CITIES[drop].lng,
      });
      setResults(asArray(data, 'trips'));
    } catch (err) {
      setSearchError(err.userMessage || err.message);
      setResults([]);
    } finally {
      setSearching(false);
    }
  };

  return (
    <>
      <NavBar subtitle="Passenger" />
      <Container className="rm-container">
        {/* ---- SEARCH ---- */}
        <Card className="rm-card p-4 mb-4">
          <div className="rm-section-label">Explore</div>
          <h5 className="mb-1">Search corridor rides</h5>
          <p className="text-muted small mb-3">
            Find published trips passing through your pickup and drop cities.
          </p>
          <Form onSubmit={search}>
            <Row className="g-3 align-items-end">
              <Col md={4}>
                <Form.Group>
                  <Form.Label>Pickup city</Form.Label>
                  <Form.Select value={pickup} onChange={(e) => setPickup(Number(e.target.value))}>
                    {CITIES.map((c, i) => (
                      <option key={c.name} value={i}>{c.name}</option>
                    ))}
                  </Form.Select>
                </Form.Group>
              </Col>
              <Col md={4}>
                <Form.Group>
                  <Form.Label>Drop city</Form.Label>
                  <Form.Select value={drop} onChange={(e) => setDrop(Number(e.target.value))}>
                    {CITIES.map((c, i) => (
                      <option key={c.name} value={i}>{c.name}</option>
                    ))}
                  </Form.Select>
                </Form.Group>
              </Col>
              <Col md={4}>
                <Button className="btn-rm w-100" type="submit" disabled={searching}>
                  {searching ? 'Searching…' : 'Search Rides'}
                </Button>
              </Col>
            </Row>
          </Form>
          {searchError && <Alert variant="danger" className="mt-3 mb-0">{searchError}</Alert>}
        </Card>

        {/* ---- RESULTS ---- */}
        <Card className="rm-card p-4 mb-4">
          <div className="rm-section-label">Available</div>
          <h5 className="mb-3">Rides on your route</h5>
          {searching && (
            <div className="empty-state">
              <Spinner animation="border" size="sm" className="me-2" /> Searching corridor rides…
            </div>
          )}
          {!searching && !searched && (
            <div className="empty-state">Search for a corridor to see available rides here.</div>
          )}
          {!searching && searched && results.length === 0 && (
            <div className="empty-state">
              No rides found for this corridor. Try a different pickup or drop city.
            </div>
          )}
          {!searching &&
            results.map((trip) => (
              <RideCard
                key={trip.tripId}
                trip={trip}
                onSelect={() => {
                  setSelectedTrip(trip);
                  setResult(null);
                  setBookingAlert(null);
                  setTimeout(() => {
                    document.getElementById('booking-section')?.scrollIntoView({ behavior: 'smooth' });
                  }, 50);
                }}
              />
            ))}
        </Card>

        {/* ---- BOOKING ---- */}
        {selectedTrip && !result && (
          <Card className="rm-card p-4 mb-4" id="booking-section">
            <div className="rm-section-label">Book</div>
            <h5 className="mb-1">Confirm your booking</h5>
            <div className="trip-summary mt-2">
              <strong>
                {selectedTrip.originName} → {selectedTrip.destinationName}
              </strong>
              {selectedTrip.driverName ? ` · ${selectedTrip.driverName}` : ''} · departs{' '}
              {formatDateTime(selectedTrip.departureTime)} · base fare {formatMoney(selectedTrip.baseFare)}
            </div>
            {bookingAlert && <Alert variant={bookingAlert.type}>{bookingAlert.message}</Alert>}
            <BookingForm
              trip={selectedTrip}
              initialPickup={pickup}
              initialDrop={drop}
              onConfirmed={async (booking, bookingType) => {
                setResult({ booking, bookingType });
                // Refresh the corridor so seat/boot counters stay truthful.
                try {
                  const data = await api.searchTrips({
                    pickupLat: CITIES[pickup].lat,
                    pickupLng: CITIES[pickup].lng,
                    dropLat: CITIES[drop].lat,
                    dropLng: CITIES[drop].lng,
                  });
                  setResults(asArray(data, 'trips'));
                } catch {
                  /* non-critical */
                }
              }}
            />
          </Card>
        )}

        {/* ---- RESULT: FARE + OTP ---- */}
        {result && (
          <section className="mb-4" id="booking-result">
            <FareOtpCard
              booking={result.booking}
              bookingType={result.bookingType}
              onBookAgain={() => {
                setResult(null);
                setSelectedTrip(null);
                window.scrollTo({ top: 0, behavior: 'smooth' });
              }}
            />
          </section>
        )}

        <p className="footer-note">RouteMate · stateless JWT auth · Spring Boot backend on http://localhost:8081</p>
      </Container>
    </>
  );
}
