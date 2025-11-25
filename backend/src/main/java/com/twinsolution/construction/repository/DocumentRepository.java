package com.twinsolution.construction.repository;

import com.twinsolution.construction.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByProjectId(Long projectId);

    List<Document> findByProjectIdAndStatus(Long projectId, String status);

    List<Document> findByProjectIdAndOrigin(Long projectId, String origin);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.createdAt >= :startDate")
    long countDocumentsCreatedAfter(LocalDateTime startDate);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.status = :status")
    long countByStatus(String status);
}
