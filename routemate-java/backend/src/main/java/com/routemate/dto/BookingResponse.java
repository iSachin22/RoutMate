package com.routemate.dto;

import com.routemate.domain.Booking;
import com.routemate.domain.BookingStatus;
import com.routemate.domain.BookingType;

import java.math.BigDecimal;
import java.util.UUID;

/** Booking payload returned to clients, including the 4-digit OTP settlement code. */
public record BookingResponse(
        UUID bookingId,
        UUID tripId,
        String passengerName,
        int bookedSeats,
        double parcelWeightKg,
        double pickupLat,
        double pickupLng,
        double dropLat,
        double dropLng,
        BookingType bookingType,
        BigDecimal calculatedFare,
        String otpCode,
        BookingStatus status) {

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getTrip().getId(),
                booking.getPassengerName(),
                booking.getBookedSeats(),
                booking.getParcelWeightKg(),
                booking.getPickupLat(),
                booking.getPickupLng(),
                booking.getDropLat(),
                booking.getDropLng(),
                booking.getBookingType(),
                booking.getCalculatedFare(),
                booking.getOtpCode(),
                booking.getStatus());
    }
}
