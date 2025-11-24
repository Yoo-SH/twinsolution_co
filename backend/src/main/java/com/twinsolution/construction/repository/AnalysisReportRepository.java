package com.twinsolution.construction.repository;

import com.twinsolution.construction.entity.AnalysisReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisReportRepository extends JpaRepository<AnalysisReport, Long> {

    List<AnalysisReport> findByDocumentId(Long documentId);

    Optional<AnalysisReport> findByDocumentIdAndId(Long documentId, Long id);

    boolean existsByDocumentId(Long documentId);
}
