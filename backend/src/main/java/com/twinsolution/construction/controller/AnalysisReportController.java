package com.twinsolution.construction.controller;

import com.twinsolution.construction.dto.AnalysisReportDto;
import com.twinsolution.construction.service.AnalysisReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnalysisReportController {

    private final AnalysisReportService analysisReportService;

    @PostMapping("/{documentId}/analysis")
    public ResponseEntity<AnalysisReportDto.Response> createAnalysisReport(@PathVariable Long documentId) {
        AnalysisReportDto.Response response = analysisReportService.createAnalysisReport(documentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{documentId}/analysis")
    public ResponseEntity<List<AnalysisReportDto.Response>> getAnalysisReports(@PathVariable Long documentId) {
        List<AnalysisReportDto.Response> reports = analysisReportService.getAnalysisReportsByDocumentId(documentId);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/{documentId}/analysis/{reportId}")
    public ResponseEntity<AnalysisReportDto.Response> getAnalysisReport(
            @PathVariable Long documentId,
            @PathVariable Long reportId) {
        AnalysisReportDto.Response report = analysisReportService.getAnalysisReportById(documentId, reportId);
        return ResponseEntity.ok(report);
    }
}
