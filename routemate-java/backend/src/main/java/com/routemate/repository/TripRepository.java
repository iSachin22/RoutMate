package com.routemate.repository;

import com.routemate.domain.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TripRepository extends JpaRepository<Trip, UUID> {

    @Query("select t from Trip t where t.driver.id = :driverId order by t.departureTime asc")
    List<Trip> findByDriverId(@Param("driverId") UUID driverId);

    @Query("select t from Trip t where t.status = com.routemate.domain.TripStatus.SCHEDULED order by t.departureTime asc")
    List<Trip> findAllScheduled();
}
