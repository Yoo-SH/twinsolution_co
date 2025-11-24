package com.twinsolution.construction.controller;

import com.twinsolution.construction.dto.DocumentDto;
import com.twinsolution.construction.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/projects/{projectId}/documents")
    public ResponseEntity<DocumentDto.Response> uploadDocument(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file) throws IOException {
        DocumentDto.Response response = documentService.uploadDocument(projectId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/projects/{projectId}/documents")
    public ResponseEntity<List<DocumentDto.Response>> getDocumentsByProject(
            @PathVariable Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String origin) {
        List<DocumentDto.Response> documents = documentService.getDocumentsByProjectId(projectId);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<DocumentDto.Response> getDocument(@PathVariable Long documentId) {
        DocumentDto.Response document = documentService.getDocumentById(documentId);
        return ResponseEntity.ok(document);
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long documentId) {
        documentService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }
}
