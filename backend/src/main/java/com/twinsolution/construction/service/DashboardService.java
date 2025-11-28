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
        // LocalDateTime.MIN 대신 충분히 오래된 날짜 사용 (2000년 1월 1일)
        LocalDateTime veryOldDate = LocalDateTime.of(2000, 1, 1, 0, 0);

        long totalDocuments = documentService.countDocumentsCreatedAfter(veryOldDate);
        long documentsLastMonth = documentService.countDocumentsCreatedAfter(thirtyDaysAgo);
        int documentsDelta = calculateDelta(totalDocuments, documentsLastMonth);

        // 진행 중인 프로젝트: "설계 진행", "서류 검토", "승인 대기"
        long inProgressProjects = projectService.countProjectsByStatus("설계 진행")
                + projectService.countProjectsByStatus("서류 검토")
                + projectService.countProjectsByStatus("승인 대기");

        // 완료된 프로젝트: "완료" 상태
        long completedProjects = projectService.countProjectsByStatus("완료");

        long totalChatSessions = chatSessionService.countSessionsCreatedAfter(veryOldDate);
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
