package com.routemate.repository;

import com.routemate.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @Query("select b from Booking b where b.trip.id = :tripId order by b.createdAt asc")
    List<Booking> findByTripId(@Param("tripId") UUID tripId);
}
