package com.tripplanner.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TripDetailResponse {

    private Long id;
    private String origin;
    private String destination;
    private int days;

    private List<DayPlanDto> dayPlans;
    private List<HotelDto> hotels;

    // ✅ ADD THIS
    private List<TransportLink> transportLinks;
}
