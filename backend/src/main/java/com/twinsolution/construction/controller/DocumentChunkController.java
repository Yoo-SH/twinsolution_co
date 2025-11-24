package com.twinsolution.construction.controller;

import com.twinsolution.construction.dto.DocumentChunkDto;
import com.twinsolution.construction.service.DocumentChunkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DocumentChunkController {

    private final DocumentChunkService documentChunkService;

    @GetMapping("/documents/{documentId}/chunks")
    public ResponseEntity<List<DocumentChunkDto.Response>> getChunksByDocument(@PathVariable Long documentId) {
        List<DocumentChunkDto.Response> chunks = documentChunkService.getChunksByDocumentId(documentId);
        return ResponseEntity.ok(chunks);
    }

    @PostMapping("/documents/{documentId}/chunks/rebuild")
    public ResponseEntity<Void> rebuildChunks(@PathVariable Long documentId) {
        documentChunkService.rebuildChunks(documentId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/settings/chunk/preview")
    public ResponseEntity<DocumentChunkDto.PreviewResponse> previewChunks(
            @RequestBody DocumentChunkDto.PreviewRequest request) {
        DocumentChunkDto.PreviewResponse response = documentChunkService.previewChunks(request);
        return ResponseEntity.ok(response);
    }
}
