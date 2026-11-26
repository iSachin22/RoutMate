import { useEffect, useState } from 'react';
import { Alert, Badge, Button, Form, Modal, Spinner } from 'react-bootstrap';
import { api, asArray, formatMoney } from '../api.js';

/** Per-trip bookings with the driver's 4-digit OTP verification box. */
export default function BookingsModal({ show, onHide, trip }) {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState(null); // { type, message }
  const [otpDrafts, setOtpDrafts] = useState({});
  const [busyId, setBusyId] = useState(null);

  useEffect(() => {
    if (!show || !trip?.tripId) return;
    setLoading(true);
    setError('');
    setNotice(null);
    setOtpDrafts({});
    api.getTripBookings(trip.tripId)
      .then((data) => setBookings(asArray(data, 'bookings')))
      .catch((err) => setError(err.userMessage || err.message))
      .finally(() => setLoading(false));
  }, [show, trip]);

  const verify = async (booking) => {
    const code = (otpDrafts[booking.bookingId] || '').trim();
    if (!/^\d{4}$/.test(code)) {
      setNotice({ type: 'warning', message: 'Enter the 4-digit OTP shown to the passenger.' });
      return;
    }
    setBusyId(booking.bookingId);
    setNotice(null);
    try {
      await api.verifyBookingOtp(booking.bookingId, code);
      setBookings((prev) =>
        prev.map((b) => (b.bookingId === booking.bookingId ? { ...b, status: 'COMPLETED' } : b))
      );
      setNotice({ type: 'success', message: 'OTP verified — booking settled.' });
    } catch (err) {
      setNotice({ type: 'danger', message: err.userMessage || err.message });
    } finally {
      setBusyId(null);
    }
  };

  return (
    <Modal show={show} onHide={onHide} centered size="lg">
      <Modal.Header closeButton>
        <Modal.Title>
          Bookings · {trip?.originName} → {trip?.destinationName}
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {loading && (
          <div className="empty-state">
            <Spinner animation="border" size="sm" className="me-2" />
            Loading bookings…
          </div>
        )}
        {!loading && error && <Alert variant="danger">{error}</Alert>}
        {!loading && !error && bookings.length === 0 && (
          <div className="empty-state">No bookings on this trip yet.</div>
        )}
        {!loading &&
          bookings.map((b) => {
            const verified = b.status === 'COMPLETED';
            return (
              <div className="booking-row" key={b.bookingId}>
                <div className="d-flex flex-wrap justify-content-between align-items-center gap-2">
                  <div>
                    <div className="fw-bold">
                      {b.bookingType} booking{' '}
                      <span className="text-muted small">#{String(b.bookingId).slice(0, 8)}</span>
                    </div>
                    <div className="small text-muted">
                      {b.bookingType === 'SEAT' ? `${b.bookedSeats} seat(s)` : `${b.parcelWeightKg} kg parcel`}
                      {' · '}Fare: <strong>{formatMoney(b.calculatedFare)}</strong>
                      {b.passengerName ? ` · ${b.passengerName}` : ''}
                    </div>
                  </div>
                  <div className="otp-verify-box">
                    {verified ? (
                      <Badge bg="success" className="verified-pill">OTP Verified ✓</Badge>
                    ) : (
                      <>
                        <Form.Control
                          className="otp-input"
                          inputMode="numeric"
                          maxLength={4}
                          placeholder="4-digit OTP"
                          aria-label="OTP code"
                          value={otpDrafts[b.bookingId] || ''}
                          onChange={(e) =>
                            setOtpDrafts((prev) => ({ ...prev, [b.bookingId]: e.target.value.replace(/\D/g, '') }))
                          }
                        />
                        <Button
                          size="sm"
                          variant="success"
                          disabled={busyId === b.bookingId}
                          onClick={() => verify(b)}
                        >
                          {busyId === b.bookingId ? '…' : 'Verify'}
                        </Button>
                      </>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        {notice && <Alert variant={notice.type} className="mt-2 py-2">{notice.message}</Alert>}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="outline-secondary" onClick={onHide}>Close</Button>
      </Modal.Footer>
    </Modal>
  );
}
