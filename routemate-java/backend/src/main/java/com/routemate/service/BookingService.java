package com.routemate.service;

import com.routemate.domain.Booking;
import com.routemate.domain.BookingStatus;
import com.routemate.domain.BookingType;
import com.routemate.domain.Trip;
import com.routemate.dto.BookingRequest;
import com.routemate.dto.BookingResponse;
import com.routemate.exception.ApiException;
import com.routemate.repository.BookingRepository;
import com.routemate.repository.TripRepository;
import com.routemate.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

/**
 * Booking creation (seat or parcel), fare calculation and OTP settlement.
 *
 * Fares (server-authoritative):
 *   SEAT   → baseFare × bookedSeats
 *   PARCEL → baseFare + parcelWeightKg × 15
 */
@Service
public class BookingService {

    private static final BigDecimal PARCEL_RATE_PER_KG = new BigDecimal("15");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository,
                          TripRepository tripRepository,
                          UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest request, UUID authenticatedPassengerId) {
        validateBookingPayload(request);

        // passengerId in the payload must match the JWT principal.
        if (request.passengerId() != null && !request.passengerId().isBlank()
                && !UUID.fromString(request.passengerId()).equals(authenticatedPassengerId)) {
            throw ApiException.forbidden("Unauthorized / Role Mismatch: passengerId does not match the authenticated passenger.");
        }

        UUID tripId = parseUuid(request.tripId(), "tripId");
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> ApiException.notFound("Trip not found: " + tripId));

        int bookedSeats = request.bookedSeats() == null ? 0 : request.bookedSeats();
        double weight = request.parcelWeightKg() == null ? 0 : request.parcelWeightKg();

        if (request.bookingType() == BookingType.SEAT) {
            if (trip.getAvailableSeats() < bookedSeats) {
                throw ApiException.badRequest("Not enough seats on this trip: "
                        + trip.getAvailableSeats() + " left, requested " + bookedSeats + ".");
            }
            trip.setAvailableSeats(trip.getAvailableSeats() - bookedSeats);
        } else {
            if (trip.getAvailableBootKg() < weight) {
                throw ApiException.badRequest("Not enough boot space on this trip: "
                        + trip.getAvailableBootKg() + " kg left, requested " + weight + " kg.");
            }
            trip.setAvailableBootKg(round1(trip.getAvailableBootKg() - weight));
        }

        Booking booking = new Booking();
        booking.setTrip(trip);
        booking.setPassengerId(authenticatedPassengerId);
        booking.setPassengerName(userRepository.findById(authenticatedPassengerId)
                .map(u -> u.getName()).orElse(null));
        booking.setBookedSeats(bookedSeats);
        booking.setParcelWeightKg(weight);
        booking.setPickupLat(request.pickupLat());
        booking.setPickupLng(request.pickupLng());
        booking.setDropLat(request.dropLat());
        booking.setDropLng(request.dropLng());
        booking.setBookingType(request.bookingType());
        booking.setCalculatedFare(calculateFare(trip, request.bookingType(), bookedSeats, weight));
        booking.setOtpCode(generateOtp());
        bookingRepository.save(booking);
        tripRepository.save(trip);
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getTripBookings(UUID tripId, UUID authenticatedDriverId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> ApiException.notFound("Trip not found: " + tripId));
        if (!trip.getDriver().getId().equals(authenticatedDriverId)) {
            throw ApiException.forbidden("Unauthorized / Role Mismatch: only the trip's driver can view its bookings.");
        }
        return bookingRepository.findByTripId(tripId).stream()
                .map(BookingResponse::from)
                .toList();
    }

    /** Driver settles a booking by verifying the passenger's 4-digit OTP. */
    @Transactional
    public void verifyOtp(UUID bookingId, String otpCode, UUID authenticatedDriverId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking not found: " + bookingId));
        if (!booking.getTrip().getDriver().getId().equals(authenticatedDriverId)) {
            throw ApiException.forbidden("Unauthorized / Role Mismatch: only the trip's driver can verify OTPs.");
        }
        if (otpCode == null || !otpCode.matches("\\d{4}")) {
            throw ApiException.badRequest("OTP must be exactly 4 digits.");
        }
        if (!booking.getOtpCode().equals(otpCode)) {
            throw ApiException.badRequest("Invalid OTP");
        }
        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);
    }

    /* ---------------- helpers ---------------- */

    private BigDecimal calculateFare(Trip trip, BookingType type, int bookedSeats, double weight) {
        BigDecimal fare = type == BookingType.SEAT
                ? trip.getBaseFare().multiply(BigDecimal.valueOf(bookedSeats))
                : trip.getBaseFare().add(PARCEL_RATE_PER_KG.multiply(BigDecimal.valueOf(weight)));
        return fare.setScale(2, RoundingMode.HALF_UP);
    }

    private String generateOtp() {
        return String.format("%04d", RANDOM.nextInt(10_000));
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private UUID parseUuid(String value, String field) {
        try {
            return UUID.fromString(value);
        } catch (Exception ex) {
            throw ApiException.badRequest(field + " must be a valid UUID.");
        }
    }

    private void validateBookingPayload(BookingRequest request) {
        if (request.tripId() == null || request.tripId().isBlank()) {
            throw ApiException.badRequest("tripId is required.");
        }
        if (request.bookingType() == null) {
            throw ApiException.badRequest("bookingType must be \"SEAT\" or \"PARCEL\".");
        }
        if (request.pickupLat() == null || request.pickupLng() == null
                || request.dropLat() == null || request.dropLng() == null
                || request.pickupLat() < -90 || request.pickupLat() > 90
                || request.dropLat() < -90 || request.dropLat() > 90
                || request.pickupLng() < -180 || request.pickupLng() > 180
                || request.dropLng() < -180 || request.dropLng() > 180) {
            throw ApiException.badRequest(
                    "pickup/drop coordinates are invalid. Latitude must be -90 to 90 and longitude -180 to 180.");
        }
        int bookedSeats = request.bookedSeats() == null ? -1 : request.bookedSeats();
        double weight = request.parcelWeightKg() == null ? -1 : request.parcelWeightKg();

        if (request.bookingType() == BookingType.SEAT) {
            if (bookedSeats < 1) {
                throw ApiException.badRequest("A SEAT booking needs bookedSeats of at least 1.");
            }
            if (weight > 0) {
                throw ApiException.badRequest("A SEAT booking must have parcelWeightKg of 0.");
            }
        } else {
            if (bookedSeats != 0) {
                throw ApiException.badRequest("A PARCEL booking must have bookedSeats of 0.");
            }
            if (weight <= 0) {
                throw ApiException.badRequest("A PARCEL booking needs parcelWeightKg greater than 0.");
            }
        }
    }
}
