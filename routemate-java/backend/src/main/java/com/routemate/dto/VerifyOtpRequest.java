package com.routemate.dto;

/**
 * POST /api/v1/bookings/{bookingId}/verify-otp — driver settles a booking
 * by verifying the passenger's 4-digit OTP at pickup.
 */
public record VerifyOtpRequest(String otpCode) {
}
