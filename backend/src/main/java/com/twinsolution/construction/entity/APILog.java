package com.twinsolution.construction.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "API_Log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class APILog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_id", nullable = false)
    private API api;

    @Column(name = "status", length = 10, nullable = false)
    private String status;

    @Column(name = "status_CODE", nullable = false)
    private Integer statusCode;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
