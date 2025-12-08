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
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final AnalysisReportRepository analysisReportRepository;
    private final SettingService settingService;
    private final WebClient ragWebClient;  // RAG 서비스 클라이언트

    private static final String UPLOAD_DIR = "uploads/documents/";

    @Transactional
    public DocumentDto.Response uploadDocument(Long projectId, MultipartFile file) throws IOException {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트", "ID", projectId));

        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String savedFileName = UUID.randomUUID().toString() + fileExtension;

        // 프로젝트별 디렉토리 생성: uploads/documents/{projectId}_{projectName}/
        String projectFolderName = sanitizeFolderName(projectId + "_" + project.getName());
        Path projectUploadPath = Paths.get(UPLOAD_DIR, projectFolderName);

        if (!Files.exists(projectUploadPath)) {
            Files.createDirectories(projectUploadPath);
            log.info("프로젝트 폴더 생성: {}", projectUploadPath);
        }

        Path filePath = projectUploadPath.resolve(savedFileName);
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

        // RAG 서비스로 문서 업로드 (비동기)
        uploadToRagServiceAsync(savedDocument, filePath);

        return convertToResponse(savedDocument);
    }

    /**
     * RAG 서비스로 문서를 비동기 업로드 (프로젝트 ID 포함)
     */
    private void uploadToRagServiceAsync(Document document, Path filePath) {
        // 비동기로 실행 (별도 스레드)
        new Thread(() -> {
            try {
                log.info("RAG 서비스로 문서 업로드 시작. Document ID: {}, Project ID: {}",
                        document.getId(), document.getProject().getId());

                var chunkSettings = settingService.getChunkSettings();

                // Multipart 요청 생성
                MultipartBodyBuilder builder = new MultipartBodyBuilder();
                builder.part("file", new FileSystemResource(filePath.toFile()));
                builder.part("document_source", document.getName());
                builder.part("project_id", document.getProject().getId().toString());  // 프로젝트 ID 추가
                builder.part("alpha", -100);  // 기본값
                builder.part("post_process_max_size", chunkSettings.getChunkSize() != null ? chunkSettings.getChunkSize() : 2000);
                builder.part("post_process_min_size", chunkSettings.getChunkOverlap() != null ? chunkSettings.getChunkOverlap() : 500);

                // RAG 서비스 호출
                String response = ragWebClient.post()
                        .uri("/api/v1/RAG/documents/upload")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(builder.build()))
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                log.info("RAG 서비스 업로드 완료. Document ID: {}, Response: {}",
                        document.getId(), response);

                // 문서 상태 업데이트
                updateDocumentStatus(document.getId(), "RAG 인덱싱 완료");

            } catch (Exception e) {
                log.error("RAG 서비스 업로드 실패. Document ID: {}, Error: {}",
                        document.getId(), e.getMessage(), e);
                updateDocumentStatus(document.getId(), "RAG 인덱싱 실패");
            }
        }).start();
    }

    /**
     * 문서 상태 업데이트
     */
    @Transactional
    public void updateDocumentStatus(Long documentId, String status) {
        try {
            Document document = documentRepository.findById(documentId).orElse(null);
            if (document != null) {
                document.setStatus(status);
                documentRepository.save(document);
            }
        } catch (Exception e) {
            log.error("문서 상태 업데이트 실패. Document ID: {}", documentId, e);
        }
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
                log.info("파일 삭제: {}", filePath);
            }
        } catch (IOException e) {
            log.error("파일 삭제 실패: {}", document.getFilePath(), e);
        }

        documentRepository.deleteById(documentId);
    }

    /**
     * 폴더명을 파일시스템에 안전한 형태로 변환
     * 특수문자, 공백 등을 '_'로 치환
     */
    private String sanitizeFolderName(String folderName) {
        // 윈도우/리눅스에서 사용 불가능한 문자 제거: \/:*?"<>|
        // 공백도 '_'로 변환
        return folderName.replaceAll("[\\\\/:*?\"<>|\\s]", "_");
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
