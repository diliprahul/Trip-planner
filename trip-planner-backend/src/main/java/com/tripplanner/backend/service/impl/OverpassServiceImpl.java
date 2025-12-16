package com.tripplanner.backend.service.impl;

import com.tripplanner.backend.service.OverpassService;
import com.tripplanner.backend.util.PlaceResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OverpassServiceImpl implements OverpassService {

    private final RestTemplate restTemplate;

    private static final String OVERPASS_URL =
            "https://overpass-api.de/api/interpreter";

    @Override
    public List<PlaceResult> getTouristPlaces(double lat, double lon) {

        String query = """
        [out:json][timeout:25];
        (
          node["tourism"~"attraction|museum|zoo|theme_park"](around:25000,%f,%f);
          node["historic"](around:25000,%f,%f);
          node["leisure"="park"](around:25000,%f,%f);
          node["natural"~"peak|waterfall|cave"](around:25000,%f,%f);
          node["amenity"="place_of_worship"]["name"](around:25000,%f,%f);
        );
        out center;
        """.formatted(lat, lon, lat, lon, lat, lon, lat, lon, lat, lon);

        return fetch(query);
    }

    @Override
    public List<PlaceResult> getHotels(double lat, double lon) {

        String query = """
        [out:json][timeout:15];
        (
          node["tourism"="hotel"](around:8000,%f,%f);
          node["tourism"="guest_house"](around:8000,%f,%f);
          node["tourism"="hostel"](around:8000,%f,%f);
        );
        out center;
        """.formatted(lat, lon, lat, lon, lat, lon);

        return fetch(query);
    }

    @SuppressWarnings("unchecked")
    private List<PlaceResult> fetch(String query) {

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<String> entity =
                    new HttpEntity<>("data=" + query, headers);

            Map<String, Object> response =
                    restTemplate.postForObject(OVERPASS_URL, entity, Map.class);

            if (response == null || !response.containsKey("elements")) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> elements =
                    (List<Map<String, Object>>) response.get("elements");

            List<PlaceResult> results = new ArrayList<>();

            for (Map<String, Object> el : elements) {

                Map<String, Object> tags =
                        (Map<String, Object>) el.get("tags");

                if (tags == null || !tags.containsKey("name")) continue;

                Double latVal = (Double) el.get("lat");
                Double lonVal = (Double) el.get("lon");

                if ((latVal == null || lonVal == null) && el.containsKey("center")) {
                    Map<String, Object> center =
                            (Map<String, Object>) el.get("center");
                    latVal = (Double) center.get("lat");
                    lonVal = (Double) center.get("lon");
                }

                if (latVal == null || lonVal == null) continue;

                // ✅ Wikimedia image support
                String imageUrl = null;
                if (tags.containsKey("wikimedia_commons")) {
                    imageUrl =
                            "https://commons.wikimedia.org/wiki/Special:FilePath/"
                                    + tags.get("wikimedia_commons");
                }

                results.add(
                        PlaceResult.builder()
                                .name((String) tags.get("name"))
                                .address((String) tags.getOrDefault("addr:full", ""))
                                .latitude(latVal)
                                .longitude(lonVal)
                                .imageUrl(imageUrl)
                                .build()
                );
            }

            return results;

        } catch (Exception e) {
            log.error("Overpass query failed", e);
            return Collections.emptyList();
        }
    }
}
