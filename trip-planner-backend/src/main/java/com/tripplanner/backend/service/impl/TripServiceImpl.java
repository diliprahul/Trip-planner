package com.tripplanner.backend.service.impl;

import com.tripplanner.backend.dto.*;
import com.tripplanner.backend.entity.*;
import com.tripplanner.backend.repository.*;
import com.tripplanner.backend.service.*;
import com.tripplanner.backend.util.GeoLocation;
import com.tripplanner.backend.util.PlaceResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final DayPlanRepository dayPlanRepository;
    private final HotelSuggestionRepository hotelSuggestionRepository;
    private final GeocodingService geocodingService;
    private final OverpassService overpassService;
    private final WikipediaImageService wikipediaImageService;

    private static final String DEFAULT_IMAGE =
            "https://upload.wikimedia.org/wikipedia/commons/d/d1/Image_not_available.png";

    // =========================================================
    // CREATE TRIP
    // =========================================================
    @Override
    public TripDetailResponse createTrip(CreateTripRequest request) {

        Trip trip = tripRepository.save(
                Trip.builder()
                        .origin(request.getOrigin())
                        .destination(request.getDestination())
                        .days(request.getDays())
                        .categories(request.getCategories())
                        .build()
        );

        return getTripById(trip.getId());
    }

    // =========================================================
    // GENERATE ITINERARY
    // =========================================================
    @Override
    public TripDetailResponse generateItinerary(Long tripId) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        dayPlanRepository.deleteByTrip(trip);
        hotelSuggestionRepository.deleteByTrip(trip);

        // ================= GEO =================
        if (trip.getLatitude() == null || trip.getLongitude() == null) {

            GeoLocation loc =
                    geocodingService.geocodeCity(trip.getDestination() + ", India");

            if (loc == null) {
                throw new RuntimeException("Unable to geocode destination");
            }

            trip.setLatitude(loc.getLatitude());
            trip.setLongitude(loc.getLongitude());
            tripRepository.save(trip);
        }

        int days = Math.max(1, trip.getDays());

        // ================= PLACES =================
        List<PlaceResult> rawPlaces = fetchPlacesSafely(trip);

        List<PlaceResult> cleaned =
                rawPlaces.stream()
                        .filter(p -> p != null && p.getName() != null)
                        .filter(p -> !isNoise(p.getName()))
                        .filter(p ->
                                p.getLatitude() != null &&
                                        p.getLongitude() != null &&
                                        p.getLatitude() >= 6 && p.getLatitude() <= 38 &&
                                        p.getLongitude() >= 68 && p.getLongitude() <= 98
                        )
                        .collect(Collectors.collectingAndThen(
                                Collectors.toMap(
                                        p -> normalizeName(p.getName()),
                                        p -> p,
                                        (a, b) -> a
                                ),
                                m -> new ArrayList<>(m.values())
                        ));

        if (cleaned.isEmpty()) {
            log.warn("No places found. Using city fallback.");
            cleaned = List.of(
                    PlaceResult.builder()
                            .name(trip.getDestination() + " City Sightseeing")
                            .latitude(trip.getLatitude())
                            .longitude(trip.getLongitude())
                            .build()
            );
        }

        Map<String, List<PlaceResult>> grouped =
                cleaned.stream()
                        .collect(Collectors.groupingBy(this::categoryOf));

        List<String> priority =
                List.of("HISTORIC", "NATURE", "MALL", "PARK", "RELIGIOUS");

        List<PlaceResult> selected = new ArrayList<>();
        Set<String> used = new HashSet<>();

        for (int day = 0; day < days; day++) {

            String category = priority.get(day % priority.size());
            List<PlaceResult> candidates =
                    grouped.getOrDefault(category, List.of());

            PlaceResult picked = null;

            for (PlaceResult p : candidates) {
                if (used.add(p.getName())) {
                    picked = p;
                    break;
                }
            }

            if (picked == null) {
                for (PlaceResult p : cleaned) {
                    if (used.add(p.getName())) {
                        picked = p;
                        break;
                    }
                }
            }

            if (picked == null) {
                picked = cleaned.get(day % cleaned.size());
            }

            selected.add(picked);
        }

        // ================= SAVE DAY PLANS =================
        List<DayPlan> plans = new ArrayList<>();

        for (int i = 0; i < selected.size(); i++) {

            PlaceResult p = selected.get(i);

            String image = DEFAULT_IMAGE;
            try {
                image = Optional.ofNullable(
                                wikipediaImageService.getImageForPlace(p.getName())
                        ).filter(s -> !s.isBlank())
                        .orElse(DEFAULT_IMAGE);
            } catch (Exception e) {
                log.warn("Wikipedia image failed for {}", p.getName());
            }

            plans.add(
                    DayPlan.builder()
                            .trip(trip)
                            .dayNumber(i + 1)
                            .placeName(p.getName())
                            .description("Popular tourist attraction in " + trip.getDestination())
                            .latitude(p.getLatitude())
                            .longitude(p.getLongitude())
                            .imageUrl(image)
                            .build()
            );
        }

        dayPlanRepository.saveAll(plans);

        // ================= HOTELS (NEW) =================
        List<PlaceResult> hotelPlaces = List.of();
        try {
            hotelPlaces = overpassService.getHotels(
                    trip.getLatitude(),
                    trip.getLongitude()
            );
        } catch (Exception e) {
            log.warn("Hotel fetch failed for {}", trip.getDestination());
        }

        List<HotelSuggestion> hotels =
                hotelPlaces.stream()
                        .filter(h -> h.getName() != null)
                        .limit(3)
                        .map(h -> HotelSuggestion.builder()
                                .trip(trip)
                                .name(h.getName())
                                .address(
                                        h.getAddress() != null
                                                ? h.getAddress()
                                                : trip.getDestination()
                                )
                                .latitude(h.getLatitude())
                                .longitude(h.getLongitude())
                                .build()
                        )
                        .toList();

        hotelSuggestionRepository.saveAll(hotels);

        return getTripById(tripId);
    }

    // =========================================================
    // FETCH
    // =========================================================
    @Override
    public TripDetailResponse getTripById(Long id) {

        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        return map(
                trip,
                dayPlanRepository.findByTripOrderByDayNumberAsc(trip),
                hotelSuggestionRepository.findByTrip(trip)
        );
    }

    @Override
    public List<TripDetailResponse> getAllTrips() {
        return tripRepository.findAll()
                .stream()
                .map(t -> getTripById(t.getId()))
                .toList();
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private List<PlaceResult> fetchPlacesSafely(Trip trip) {
        try {
            return Optional.ofNullable(
                    overpassService.getTouristPlaces(
                            trip.getLatitude(),
                            trip.getLongitude()
                    )
            ).orElse(List.of());
        } catch (Exception e) {
            return List.of();
        }
    }

    private boolean isNoise(String name) {
        String n = name.toLowerCase();
        return n.contains("office") || n.contains("school") || n.contains("college");
    }

    private String normalizeName(String name) {
        return name.toLowerCase()
                .replace("entrance", "")
                .replace("gate", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String categoryOf(PlaceResult p) {
        String n = p.getName().toLowerCase();
        if (n.contains("hill") || n.contains("lake")) return "NATURE";
        if (n.contains("fort") || n.contains("cave")) return "HISTORIC";
        if (n.contains("park")) return "PARK";
        if (n.contains("mall")) return "MALL";
        if (n.contains("temple") || n.contains("church")) return "RELIGIOUS";
        return "OTHER";
    }

    private String buildMapsUrl(String name, Double lat, Double lng, String city) {
        if (lat != null && lng != null) {
            return "https://www.google.com/maps/search/?api=1&query=" + lat + "," + lng;
        }
        return "https://www.google.com/maps/search/?api=1&query="
                + (name + " " + city).replace(" ", "+");
    }

    private String buildHotelSearchUrl(String city) {
        return "https://www.google.com/maps/search/?api=1&query=hotels+in+"
                + city.replace(" ", "+");
    }

    // =========================================================
    // MAPPER
    // =========================================================
    private TripDetailResponse map(
            Trip t,
            List<DayPlan> plans,
            List<HotelSuggestion> hotels
    ) {

        List<HotelDto> hotelDtos = new ArrayList<>();

        hotels.stream()
                .limit(3)
                .forEach(h -> hotelDtos.add(
                        HotelDto.builder()
                                .name(h.getName())
                                .address(h.getAddress())
                                .searchUrl(
                                        buildMapsUrl(
                                                h.getName(),
                                                h.getLatitude(),
                                                h.getLongitude(),
                                                t.getDestination()
                                        )
                                )
                                .build()
                ));

        hotelDtos.add(
                HotelDto.builder()
                        .name("View more hotels in " + t.getDestination())
                        .searchUrl(buildHotelSearchUrl(t.getDestination()))
                        .build()
        );

        return TripDetailResponse.builder()
                .id(t.getId())
                .origin(t.getOrigin())
                .destination(t.getDestination())
                .days(t.getDays())
                .dayPlans(
                        plans.stream()
                                .map(dp -> DayPlanDto.builder()
                                        .dayNumber(dp.getDayNumber())
                                        .placeName(dp.getPlaceName())
                                        .description(dp.getDescription())
                                        .imageUrl(dp.getImageUrl())
                                        .mapsUrl(
                                                buildMapsUrl(
                                                        dp.getPlaceName(),
                                                        dp.getLatitude(),
                                                        dp.getLongitude(),
                                                        t.getDestination()
                                                )
                                        )
                                        .build())
                                .toList()
                )
                .hotels(hotelDtos)
                .build();
    }
}
