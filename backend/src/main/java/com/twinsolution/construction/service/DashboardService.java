package com.twinsolution.construction.service;

import com.twinsolution.construction.dto.DashboardDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final ProjectService projectService;
    private final DocumentService documentService;
    private final ChatSessionService chatSessionService;

    public DashboardDto.SummaryResponse getDashboardSummary() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        long totalDocuments = documentService.countDocumentsCreatedAfter(LocalDateTime.MIN);
        long documentsLastMonth = documentService.countDocumentsCreatedAfter(thirtyDaysAgo);
        int documentsDelta = calculateDelta(totalDocuments, documentsLastMonth);

        long inProgressProjects = projectService.countProjectsByStatus("진행중");
        long completedProjects = projectService.countProjectsByStatus("완료");

        long totalChatSessions = chatSessionService.countSessionsCreatedAfter(LocalDateTime.MIN);
        long chatSessionsLastMonth = chatSessionService.countSessionsCreatedAfter(thirtyDaysAgo);
        int chatSessionsDelta = calculateDelta(totalChatSessions, chatSessionsLastMonth);

        return DashboardDto.SummaryResponse.builder()
                .totalDocuments(totalDocuments)
                .documentsDelta(documentsDelta)
                .inProgressProjects(inProgressProjects)
                .completedProjects(completedProjects)
                .chatSessionsCount(totalChatSessions)
                .chatSessionsDelta(chatSessionsDelta)
                .build();
    }

    private int calculateDelta(long total, long recent) {
        if (total == 0) return 0;
        return (int) ((recent * 100) / total);
    }
}
