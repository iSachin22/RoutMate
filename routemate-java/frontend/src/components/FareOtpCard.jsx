import { Button } from 'react-bootstrap';
import { formatMoney } from '../api.js';

/**
 * The prominent post-booking card: live fare + 4-digit OTP.
 */
export default function FareOtpCard({ booking, bookingType, onBookAgain }) {
  const otp = String(booking?.otpCode ?? '');
  const otpDigits = /^\d{4}$/.test(otp) ? otp.split('') : null;

  return (
    <div className="result-card">
      <div className="result-kicker">
        {bookingType === 'SEAT' ? 'Seat booking' : 'Parcel booking'}
        {booking?.bookingId ? ` · #${String(booking.bookingId).slice(0, 8)}` : ''}
      </div>
      <div className="result-fare">{formatMoney(booking?.calculatedFare)}</div>
      <div className="small" style={{ opacity: 0.85 }}>Total fare (calculated live by the server)</div>
      <div className="result-otp-label">Your 4-digit OTP — show this to your driver</div>
      <div>
        {otpDigits ? (
          otpDigits.map((d, i) => (
            <span className="otp-digit" key={i}>{d}</span>
          ))
        ) : (
          <span className="otp-digit-wide">{otp || '—'}</span>
        )}
      </div>
      <div className="result-meta">
        {booking?.bookedSeats !== undefined && (
          <>Seats: <strong>{booking.bookedSeats}</strong> · </>
        )}
        {booking?.parcelWeightKg !== undefined && (
          <>Weight: <strong>{booking.parcelWeightKg} kg</strong> · </>
        )}
        Status: <strong>{booking?.status || 'CONFIRMED'}</strong>
        {booking?.tripId ? ` · Trip: ${String(booking.tripId).slice(0, 8)}…` : ''}
      </div>
      <Button className="btn-on-dark" onClick={onBookAgain}>Book another ride</Button>
    </div>
  );
}
