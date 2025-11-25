package com.twinsolution.construction.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "API")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class API {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "base_url", length = 255, nullable = false)
    private String baseUrl;

    @Column(name = "method", length = 10, nullable = false)
    private String method;

    @Column(name = "auth_key", length = 255)
    private String authKey;

    @Column(name = "status", length = 10, nullable = false)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "api", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<APILog> apiLogs = new ArrayList<>();
}
