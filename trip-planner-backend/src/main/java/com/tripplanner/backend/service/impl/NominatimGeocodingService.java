package com.tripplanner.backend.service.impl;

import com.tripplanner.backend.service.GeocodingService;
import com.tripplanner.backend.util.GeoLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NominatimGeocodingService implements GeocodingService {

    private final RestTemplate restTemplate;

    @Override
    public GeoLocation geocodeCity(String city) {

        String url =
                "https://nominatim.openstreetmap.org/search" +
                        "?q=" + city +
                        "&format=json&limit=1";

        List<Map<String, String>> res =
                restTemplate.getForObject(url, List.class);

        if (res == null || res.isEmpty()) {
            return new GeoLocation(0.0, 0.0);
        }

        return new GeoLocation(
                Double.parseDouble(res.get(0).get("lat")),
                Double.parseDouble(res.get(0).get("lon"))
        );
    }
}
