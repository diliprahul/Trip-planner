package com.tripplanner.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "trips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String origin;
    private String destination;
    private Integer days;

    private Double latitude;
    private Double longitude;

    // ✅ USER SELECTED CATEGORIES
    @ElementCollection
    @CollectionTable(name = "trip_categories", joinColumns = @JoinColumn(name = "trip_id"))
    @Column(name = "category")
    private List<String> categories;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
