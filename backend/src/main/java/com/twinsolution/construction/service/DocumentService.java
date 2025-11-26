package com.twinsolution.construction.service;

import com.twinsolution.construction.dto.DocumentDto;
import com.twinsolution.construction.entity.Document;
import com.twinsolution.construction.entity.Project;
import com.twinsolution.construction.exception.BadRequestException;
import com.twinsolution.construction.exception.ResourceNotFoundException;
import com.twinsolution.construction.repository.AnalysisReportRepository;
import com.twinsolution.construction.repository.DocumentChunkRepository;
import com.twinsolution.construction.repository.DocumentRepository;
import com.twinsolution.construction.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final AnalysisReportRepository analysisReportRepository;
    private final SettingService settingService;

    private static final String UPLOAD_DIR = "uploads/documents/";

    @Transactional
    public DocumentDto.Response uploadDocument(Long projectId, MultipartFile file) throws IOException {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트", "ID", projectId));

        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String savedFileName = UUID.randomUUID().toString() + fileExtension;

        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(savedFileName);
        Files.copy(file.getInputStream(), filePath);

        var chunkSettings = settingService.getChunkSettings();

        Document document = Document.builder()
                .project(project)
                .name(originalFilename)
                .fileType(fileExtension.replace(".", "").toUpperCase())
                .filePath(filePath.toString())
                .origin("uploaded")
                .status("업로드됨")
                .chunkSize(chunkSettings.getChunkSize())
                .chunkOverlap(chunkSettings.getChunkOverlap())
                .build();

        Document savedDocument = documentRepository.save(document);
        return convertToResponse(savedDocument);
    }

    @Transactional
    public DocumentDto.Response createGeneratedDocument(DocumentDto.Request request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트", "ID", request.getProjectId()));

        Document document = Document.builder()
                .project(project)
                .name(request.getName())
                .fileType(request.getFileType())
                .filePath(request.getFilePath())
                .origin("generated")
                .status(request.getStatus() != null ? request.getStatus() : "분석완료")
                .build();

        Document savedDocument = documentRepository.save(document);
        return convertToResponse(savedDocument);
    }

    public List<DocumentDto.Response> getDocumentsByProjectId(Long projectId) {
        List<Document> documents = documentRepository.findByProjectId(projectId);
        return documents.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public DocumentDto.Response getDocumentById(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("문서", "ID", documentId));
        return convertToResponse(document);
    }

    @Transactional
    public void deleteDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("문서", "ID", documentId));

        try {
            Path filePath = Paths.get(document.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            // Log error but continue with deletion
        }

        documentRepository.deleteById(documentId);
    }

    public long countDocumentsCreatedAfter(LocalDateTime date) {
        return documentRepository.countDocumentsCreatedAfter(date);
    }

    public long countDocumentsByStatus(String status) {
        return documentRepository.countByStatus(status);
    }

    private DocumentDto.Response convertToResponse(Document document) {
        long chunkCount = documentChunkRepository.countByDocumentId(document.getId());
        boolean hasAnalysisReport = analysisReportRepository.existsByDocumentId(document.getId());

        return DocumentDto.Response.builder()
                .id(document.getId())
                .projectId(document.getProject().getId())
                .name(document.getName())
                .fileType(document.getFileType())
                .filePath(document.getFilePath())
                .origin(document.getOrigin())
                .status(document.getStatus())
                .chunkSize(document.getChunkSize())
                .chunkOverlap(document.getChunkOverlap())
                .chunkCount(chunkCount)
                .hasAnalysisReport(hasAnalysisReport)
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
