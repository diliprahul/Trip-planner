package com.tripplanner.backend.service;

import com.tripplanner.backend.dto.*;

import java.util.List;

public interface TripService {

    TripDetailResponse createTrip(CreateTripRequest request);

    TripDetailResponse generateItinerary(Long tripId);

    TripDetailResponse getTripById(Long id);

    List<TripDetailResponse> getAllTrips();
}
