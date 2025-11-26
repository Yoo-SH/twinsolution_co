package com.twinsolution.construction.service;

import com.twinsolution.construction.dto.DocumentChunkDto;
import com.twinsolution.construction.entity.Document;
import com.twinsolution.construction.entity.DocumentChunk;
import com.twinsolution.construction.exception.ResourceNotFoundException;
import com.twinsolution.construction.repository.DocumentChunkRepository;
import com.twinsolution.construction.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentChunkService {

    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentRepository documentRepository;

    public List<DocumentChunkDto.Response> getChunksByDocumentId(Long documentId) {
        List<DocumentChunk> chunks = documentChunkRepository.findByDocumentIdOrderByIndexAsc(documentId);
        return chunks.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void rebuildChunks(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("문서", "ID", documentId));

        documentChunkRepository.deleteByDocumentId(documentId);

        // TODO: Implement actual chunking logic here
        // This is a placeholder for the actual implementation
        document.setStatus("분할완료");
    }

    public DocumentChunkDto.PreviewResponse previewChunks(DocumentChunkDto.PreviewRequest request) {
        List<DocumentChunkDto.ChunkPreview> chunks = performChunking(
                request.getText(),
                request.getChunkSize() != null ? request.getChunkSize() : 1000,
                request.getChunkOverlap() != null ? request.getChunkOverlap() : 200,
                request.getSeparators() != null ? request.getSeparators() : List.of("\n\n", "\n", " ")
        );

        double averageLength = chunks.stream()
                .mapToInt(DocumentChunkDto.ChunkPreview::getLength)
                .average()
                .orElse(0.0);

        double overlapRatio = request.getChunkOverlap() != null && request.getChunkSize() != null
                ? (double) request.getChunkOverlap() / request.getChunkSize() * 100
                : 0.0;

        return DocumentChunkDto.PreviewResponse.builder()
                .chunks(chunks)
                .totalChunks(chunks.size())
                .averageLength(averageLength)
                .overlapRatio(overlapRatio)
                .build();
    }

    private List<DocumentChunkDto.ChunkPreview> performChunking(String text, int chunkSize, int overlap, List<String> separators) {
        List<DocumentChunkDto.ChunkPreview> chunks = new ArrayList<>();
        int index = 0;
        int position = 0;

        while (position < text.length()) {
            int end = Math.min(position + chunkSize, text.length());
            String chunkContent = text.substring(position, end);

            chunks.add(DocumentChunkDto.ChunkPreview.builder()
                    .index(index++)
                    .content(chunkContent)
                    .length(chunkContent.length())
                    .build());

            position += (chunkSize - overlap);
            if (position >= text.length()) {
                break;
            }
        }

        return chunks;
    }

    private DocumentChunkDto.Response convertToResponse(DocumentChunk chunk) {
        return DocumentChunkDto.Response.builder()
                .id(chunk.getId())
                .documentId(chunk.getDocument().getId())
                .index(chunk.getIndex())
                .content(chunk.getContent())
                .createdAt(chunk.getCreatedAt())
                .build();
    }
}
