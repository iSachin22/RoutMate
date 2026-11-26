package com.routemate.controller;

import com.routemate.dto.BookingRequest;
import com.routemate.dto.BookingResponse;
import com.routemate.dto.MessageResponse;
import com.routemate.dto.VerifyOtpRequest;
import com.routemate.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Booking endpoints: create (passenger), driver's booking feed, OTP settlement. */
@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController extends CurrentUserController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /** POST /api/v1/bookings — create a SEAT or PARCEL booking (PASSENGER only). */
    @PostMapping
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<BookingResponse> createBooking(@RequestBody BookingRequest request,
                                                         Authentication authentication) {
        return ResponseEntity.ok(bookingService.createBooking(request, currentUserId(authentication)));
    }

    /** GET /api/v1/bookings/trip/{tripId} — bookings of a trip (its DRIVER only). */
    @GetMapping("/trip/{tripId}")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<Map<String, List<BookingResponse>>> tripBookings(@PathVariable UUID tripId,
                                                                          Authentication authentication) {
        return ResponseEntity.ok(Map.of("bookings",
                bookingService.getTripBookings(tripId, currentUserId(authentication))));
    }

    /** POST /api/v1/bookings/{bookingId}/verify-otp — OTP settlement (its DRIVER only). */
    @PostMapping("/{bookingId}/verify-otp")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<MessageResponse> verifyOtp(@PathVariable UUID bookingId,
                                                     @RequestBody VerifyOtpRequest request,
                                                     Authentication authentication) {
        bookingService.verifyOtp(bookingId, request.otpCode(), currentUserId(authentication));
        return ResponseEntity.ok(new MessageResponse("VERIFIED"));
    }
}
