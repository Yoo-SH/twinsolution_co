package com.twinsolution.construction.service;

import com.twinsolution.construction.dto.ProjectDto;
import com.twinsolution.construction.entity.Project;
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
                .status(request.getStatus() != null ? request.getStatus() : "진행중")
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
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
        return convertToResponse(project);
    }

    @Transactional
    public ProjectDto.Response updateProject(Long projectId, ProjectDto.UpdateRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));

        if (request.getName() != null) {
            project.setName(request.getName());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
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

    private ProjectDto.Response convertToResponse(Project project) {
        LocalDateTime latestSessionAt = project.getChatSessions().stream()
                .map(session -> session.getUpdatedAt())
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return ProjectDto.Response.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .status(project.getStatus())
                .progress(calculateProgress(project))
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
