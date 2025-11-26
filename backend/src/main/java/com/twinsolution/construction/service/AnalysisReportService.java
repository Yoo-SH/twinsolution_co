package com.twinsolution.construction.service;

import com.twinsolution.construction.dto.AnalysisReportDto;
import com.twinsolution.construction.entity.AnalysisReport;
import com.twinsolution.construction.entity.Document;
import com.twinsolution.construction.exception.ResourceNotFoundException;
import com.twinsolution.construction.repository.AnalysisReportRepository;
import com.twinsolution.construction.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisReportService {

    private final AnalysisReportRepository analysisReportRepository;
    private final DocumentRepository documentRepository;

    @Transactional
    public AnalysisReportDto.Response createAnalysisReport(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("문서", "ID", documentId));

        // TODO: Implement actual AI analysis logic
        String analysisContent = generateAnalysis(document);

        AnalysisReport report = AnalysisReport.builder()
                .document(document)
                .content(analysisContent)
                .build();

        AnalysisReport savedReport = analysisReportRepository.save(report);
        document.setStatus("분석완료");

        return convertToResponse(savedReport);
    }

    public List<AnalysisReportDto.Response> getAnalysisReportsByDocumentId(Long documentId) {
        List<AnalysisReport> reports = analysisReportRepository.findByDocumentId(documentId);
        return reports.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public AnalysisReportDto.Response getAnalysisReportById(Long documentId, Long reportId) {
        AnalysisReport report = analysisReportRepository.findByDocumentIdAndId(documentId, reportId)
                .orElseThrow(() -> new ResourceNotFoundException("분석 리포트", "ID", reportId));
        return convertToResponse(report);
    }

    private String generateAnalysis(Document document) {
        // TODO: Implement actual LLM-based analysis
        return "Analysis report for document: " + document.getName();
    }

    private AnalysisReportDto.Response convertToResponse(AnalysisReport report) {
        return AnalysisReportDto.Response.builder()
                .id(report.getId())
                .documentId(report.getDocument().getId())
                .content(report.getContent())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
