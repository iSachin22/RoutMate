package com.routemate.dto;

import com.routemate.domain.BookingType;

/**
 * POST /api/v1/bookings — create a seat or parcel booking.
 * SEAT:   bookedSeats &gt;= 1, parcelWeightKg = 0.
 * PARCEL: bookedSeats = 0, parcelWeightKg &gt; 0.
 */
public record BookingRequest(
        String tripId,
        String passengerId,
        Integer bookedSeats,
        Double parcelWeightKg,
        Double pickupLat,
        Double pickupLng,
        Double dropLat,
        Double dropLng,
        BookingType bookingType) {
}
