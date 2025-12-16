package com.tripplanner.backend.service.impl;

import com.tripplanner.backend.dto.*;
import com.tripplanner.backend.entity.*;
import com.tripplanner.backend.repository.*;
import com.tripplanner.backend.service.*;
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

    // ---------------- CREATE TRIP ----------------
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

        return map(trip, List.of(), List.of());
    }

    // ---------------- GENERATE ITINERARY ----------------
    @Override
    public TripDetailResponse generateItinerary(Long tripId) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        try {
            // 1. GEOCODE
            if (trip.getLatitude() == null || trip.getLongitude() == null) {
                var loc = geocodingService.geocodeCity(trip.getDestination());
                trip.setLatitude(loc.getLatitude());
                trip.setLongitude(loc.getLongitude());
                tripRepository.save(trip);
            }

            int requestedDays = Math.max(1, trip.getDays());

            // 2. FETCH PLACES
            List<PlaceResult> rawPlaces =
                    overpassService.getTouristPlaces(trip.getLatitude(), trip.getLongitude());

            if (rawPlaces == null || rawPlaces.isEmpty()) {
                throw new RuntimeException("No tourist places found");
            }

            // 3. CLEAN DATA
            List<PlaceResult> cleaned = rawPlaces.stream()
                    .filter(p -> p.getName() != null)
                    .filter(p -> isValidPlaceName(p.getName()))
                    .filter(p -> !isNoise(p.getName()))
                    .collect(Collectors.toMap(
                            p -> p.getName().toLowerCase(),
                            p -> p,
                            (a, b) -> a
                    ))
                    .values()
                    .stream()
                    .toList();

            if (cleaned.isEmpty()) {
                throw new RuntimeException("All tourist places filtered out");
            }

            // 4. GROUP BY CATEGORY
            Map<String, List<PlaceResult>> grouped =
                    cleaned.stream().collect(Collectors.groupingBy(this::categoryOf));

            // 5. USER CATEGORIES
            List<String> categories = trip.getCategories();
            if (categories == null || categories.isEmpty()) {
                categories = List.of("NATURE", "HISTORIC", "MUSEUM", "MALL", "PARK", "RELIGIOUS");
            }

            List<PlaceResult> selected = new ArrayList<>();
            Set<String> used = new HashSet<>();

            // 6A. STRICT PREFERENCE MATCH
            for (String cat : categories) {
                if (selected.size() >= requestedDays) break;

                List<PlaceResult> list = grouped.get(cat);
                if (list == null) continue;

                for (PlaceResult p : list) {
                    if (used.contains(p.getName())) continue;

                    selected.add(p);
                    used.add(p.getName());
                    break;
                }
            }

            // 6B. PRIORITY FILL (NON-RELIGIOUS ONLY)
            if (selected.size() < requestedDays) {

                List<PlaceResult> priorityPool = cleaned.stream()
                        .filter(p -> !used.contains(p.getName()))
                        .filter(p -> !categoryOf(p).equals("RELIGIOUS"))
                        .toList();

                for (PlaceResult p : priorityPool) {
                    if (selected.size() >= requestedDays) break;

                    selected.add(p);
                    used.add(p.getName());
                }
            }

            if (selected.isEmpty()) {
                throw new RuntimeException("No places available for itinerary");
            }

            // 7. CLEAR OLD DATA
            dayPlanRepository.deleteAll(
                    dayPlanRepository.findByTripOrderByDayNumberAsc(trip)
            );
            hotelSuggestionRepository.deleteAll(
                    hotelSuggestionRepository.findByTrip(trip)
            );

            // 8. SAVE DAY PLANS
            List<DayPlan> plans = new ArrayList<>();
            int generatedDays = Math.min(requestedDays, selected.size());

            for (int i = 0; i < generatedDays; i++) {
                PlaceResult p = selected.get(i);

                String imageUrl;
                try {
                    imageUrl = wikipediaImageService.getImageForPlace(p.getName());
                } catch (Exception e) {
                    imageUrl = DEFAULT_IMAGE;
                }
                if (imageUrl == null || imageUrl.isBlank()) {
                    imageUrl = DEFAULT_IMAGE;
                }

                plans.add(
                        DayPlan.builder()
                                .trip(trip)
                                .dayNumber(i + 1)
                                .placeName(p.getName())
                                .description(buildDescription(p, trip))
                                .latitude(p.getLatitude())
                                .longitude(p.getLongitude())
                                .imageUrl(imageUrl)
                                .build()
                );
            }

            dayPlanRepository.saveAll(plans);

            // 9. HOTELS
            List<PlaceResult> hotels =
                    overpassService.getHotels(trip.getLatitude(), trip.getLongitude());

            if (hotels == null || hotels.isEmpty()) {
                hotels = List.of(
                        PlaceResult.builder()
                                .name("City Center Hotel")
                                .address("Central " + trip.getDestination())
                                .latitude(trip.getLatitude())
                                .longitude(trip.getLongitude())
                                .build()
                );
            }

            List<HotelSuggestion> hotelEntities = hotels.stream()
                    .limit(6)
                    .map(h -> HotelSuggestion.builder()
                            .trip(trip)
                            .name(h.getName())
                            .address(h.getAddress())
                            .latitude(h.getLatitude())
                            .longitude(h.getLongitude())
                            .build())
                    .toList();

            hotelSuggestionRepository.saveAll(hotelEntities);

        } catch (Exception e) {
            log.error("Itinerary generation failed", e);
            throw new RuntimeException(e.getMessage());
        }

        return getTripById(tripId);
    }

    // ---------------- HELPERS ----------------

    private boolean isValidPlaceName(String name) {
        return name.toLowerCase().trim().contains(" ");
    }

    private boolean isNoise(String name) {
        String n = name.toLowerCase();
        return n.contains("office")
                || n.contains("hostel")
                || n.contains("school")
                || n.contains("college");
    }

    private String categoryOf(PlaceResult p) {
        String n = p.getName().toLowerCase();
        if (n.contains("waterfall") || n.contains("lake") || n.contains("hill")) return "NATURE";
        if (n.contains("fort") || n.contains("cave") || n.contains("monument")) return "HISTORIC";
        if (n.contains("museum")) return "MUSEUM";
        if (n.contains("mall")) return "MALL";
        if (n.contains("park")) return "PARK";
        if (n.contains("temple") || n.contains("church") || n.contains("mosque")) return "RELIGIOUS";
        return "OTHER";
    }

    private String buildDescription(PlaceResult p, Trip trip) {
        return "Famous tourist attraction in and around " + trip.getDestination() + ".";
    }

    // ---------------- FETCH ----------------
    @Override
    public TripDetailResponse getTripById(Long id) {
        Trip t = tripRepository.findById(id).orElseThrow();
        return map(
                t,
                dayPlanRepository.findByTripOrderByDayNumberAsc(t),
                hotelSuggestionRepository.findByTrip(t)
        );
    }

    @Override
    public List<TripDetailResponse> getAllTrips() {
        return tripRepository.findAll()
                .stream()
                .map(t -> getTripById(t.getId()))
                .toList();
    }

    // ---------------- MAPPER ----------------
    private TripDetailResponse map(
            Trip t, List<DayPlan> d, List<HotelSuggestion> h) {

        return TripDetailResponse.builder()
                .id(t.getId())
                .origin(t.getOrigin())
                .destination(t.getDestination())
                .days(t.getDays())
                .dayPlans(
                        d.stream().map(dp ->
                                DayPlanDto.builder()
                                        .dayNumber(dp.getDayNumber())
                                        .placeName(dp.getPlaceName())
                                        .description(dp.getDescription())
                                        .imageUrl(dp.getImageUrl())
                                        .build()
                        ).toList()
                )
                .hotels(
                        h.stream().map(ht ->
                                HotelDto.builder()
                                        .name(ht.getName())
                                        .address(ht.getAddress())
                                        .build()
                        ).toList()
                )
                .build();
    }
}
