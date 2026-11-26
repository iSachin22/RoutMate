package com.routemate.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A published trip. {@code routeWaypoints} (ordered, &gt;= 2 pairs) forms the
 * route polyline used for corridor search.
 */
@Entity
@Table(name = "trips")
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @Column(name = "origin_name", nullable = false, length = 120)
    private String originName;

    @Column(name = "destination_name", nullable = false, length = 120)
    private String destinationName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "trip_waypoints", joinColumns = @JoinColumn(name = "trip_id"))
    @OrderColumn(name = "seq")
    private List<Waypoint> routeWaypoints = new ArrayList<>();

    @Column(name = "available_seats", nullable = false)
    private int availableSeats;

    @Column(name = "available_boot_kg", nullable = false)
    private double availableBootKg;

    @Column(name = "base_fare", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseFare;

    @Column(name = "departure_time", nullable = false)
    private Instant departureTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripStatus status = TripStatus.SCHEDULED;

    public Trip() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getDriver() {
        return driver;
    }

    public void setDriver(User driver) {
        this.driver = driver;
    }

    public String getOriginName() {
        return originName;
    }

    public void setOriginName(String originName) {
        this.originName = originName;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public List<Waypoint> getRouteWaypoints() {
        return routeWaypoints;
    }

    public void setRouteWaypoints(List<Waypoint> routeWaypoints) {
        this.routeWaypoints = routeWaypoints;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public double getAvailableBootKg() {
        return availableBootKg;
    }

    public void setAvailableBootKg(double availableBootKg) {
        this.availableBootKg = availableBootKg;
    }

    public BigDecimal getBaseFare() {
        return baseFare;
    }

    public void setBaseFare(BigDecimal baseFare) {
        this.baseFare = baseFare;
    }

    public Instant getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(Instant departureTime) {
        this.departureTime = departureTime;
    }

    public TripStatus getStatus() {
        return status;
    }

    public void setStatus(TripStatus status) {
        this.status = status;
    }
}
