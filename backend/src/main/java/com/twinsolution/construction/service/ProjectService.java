package com.twinsolution.construction.service;

import com.twinsolution.construction.dto.ProjectDto;
import com.twinsolution.construction.entity.ChatSession;
import com.twinsolution.construction.entity.Project;
import com.twinsolution.construction.exception.ResourceNotFoundException;
import com.twinsolution.construction.repository.ChatSessionRepository;
import com.twinsolution.construction.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ChatSessionRepository chatSessionRepository;

    @Transactional
    public ProjectDto.Response createProject(ProjectDto.Request request) {
        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .location(request.getLocation())
                .zoning(request.getZoning())
                .usage(request.getUsage())
                .totalFloorArea(request.getTotalFloorArea())
                .floors(request.getFloors())
                .parkingSpaces(request.getParkingSpaces())
                .status(request.getStatus() != null ? request.getStatus() : "초기단계")
                .llmProvider(request.getLlmProvider() != null ? request.getLlmProvider() : "OPENAI")
                .modelName(request.getModelName() != null ? request.getModelName() : "gpt-3.5-turbo")
                .build();

        Project savedProject = projectRepository.save(project);
        return convertToResponse(savedProject);
    }

    public List<ProjectDto.Response> getRecentProjects(int limit) {
        List<Project> projects = projectRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit));
        return projects.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public ProjectDto.Response getProjectById(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트", "ID", projectId));
        return convertToResponse(project);
    }

    @Transactional
    public ProjectDto.Response updateProject(Long projectId, ProjectDto.UpdateRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트", "ID", projectId));

        if (request.getName() != null) {
            project.setName(request.getName());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getLocation() != null) {
            project.setLocation(request.getLocation());
        }
        if (request.getZoning() != null) {
            project.setZoning(request.getZoning());
        }
        if (request.getUsage() != null) {
            project.setUsage(request.getUsage());
        }
        if (request.getTotalFloorArea() != null) {
            project.setTotalFloorArea(request.getTotalFloorArea());
        }
        if (request.getFloors() != null) {
            project.setFloors(request.getFloors());
        }
        if (request.getParkingSpaces() != null) {
            project.setParkingSpaces(request.getParkingSpaces());
        }
        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
        }
        if (request.getLlmProvider() != null) {
            project.setLlmProvider(request.getLlmProvider());
        }
        if (request.getModelName() != null) {
            project.setModelName(request.getModelName());
        }

        return convertToResponse(project);
    }

    @Transactional
    public void deleteProject(Long projectId) {
        projectRepository.deleteById(projectId);
    }

    public long countProjectsByStatus(String status) {
        return projectRepository.countByStatus(status);
    }

    /**
     * 프로젝트의 채팅 세션을 가져오거나 없으면 생성합니다.
     * 프로젝트마다 하나의 세션만 유지됩니다.
     */
    @Transactional
    public Long getOrCreateSessionForProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트", "ID", projectId));

        // 프로젝트의 기존 세션 찾기
        List<ChatSession> sessions = chatSessionRepository.findByProjectIdOrderByUpdatedAtDesc(projectId);

        if (!sessions.isEmpty()) {
            // 기존 세션이 있으면 첫 번째(가장 최근) 세션 반환
            return sessions.get(0).getId();
        }

        // 세션이 없으면 새로 생성
        String defaultQuickQuestions = "[\"준주거지역에 필요한 서류는?\", \"내진설계 의무 대상\", \"주차장 설치 기준\", \"건축허가신청서 작성 방법\"]";

        ChatSession newSession = ChatSession.builder()
                .project(project)
                .quickQuestion(defaultQuickQuestions)
                .build();

        ChatSession savedSession = chatSessionRepository.save(newSession);
        return savedSession.getId();
    }

    private ProjectDto.Response convertToResponse(Project project) {
        LocalDateTime latestSessionAt = project.getChatSessions().stream()
                .map(session -> session.getUpdatedAt())
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return ProjectDto.Response.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .location(project.getLocation())
                .zoning(project.getZoning())
                .usage(project.getUsage())
                .totalFloorArea(project.getTotalFloorArea())
                .floors(project.getFloors())
                .parkingSpaces(project.getParkingSpaces())
                .status(project.getStatus())
                .progress(calculateProgress(project))
                .llmProvider(project.getLlmProvider())
                .modelName(project.getModelName())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .latestChatSessionAt(latestSessionAt)
                .build();
    }

    private Integer calculateProgress(Project project) {
        long totalDocuments = project.getDocuments().size();
        if (totalDocuments == 0) {
            return 0;
        }
        long completedDocuments = project.getDocuments().stream()
                .filter(doc -> "분석완료".equals(doc.getStatus()))
                .count();
        return (int) ((completedDocuments * 100) / totalDocuments);
    }
}
