package com.twinsolution.construction.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Document")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "file_type", length = 10, nullable = false)
    private String fileType;

    @Column(name = "file_path", length = 255, nullable = false)
    private String filePath;

    @Column(name = "origin", length = 10, nullable = false)
    private String origin;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "chunk_size")
    private Integer chunkSize;

    @Column(name = "chunck_overlap")
    private Integer chunkOverlap;

    @Column(name = "actual_chunk_count")
    private Integer actualChunkCount;  // RAG 서비스에서 실제 생성된 청크 개수

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DocumentChunk> chunks = new ArrayList<>();

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AnalysisReport> analysisReports = new ArrayList<>();
}
