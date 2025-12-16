package com.tripplanner.backend.repository;

import com.tripplanner.backend.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, Long> {
}
