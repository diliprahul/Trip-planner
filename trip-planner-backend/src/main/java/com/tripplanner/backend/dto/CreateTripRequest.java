package com.tripplanner.backend.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
public class CreateTripRequest {

    private String origin;
    private String destination;
    private Integer days;

    // ✅ MUST EXIST
    private List<String> categories;
}
