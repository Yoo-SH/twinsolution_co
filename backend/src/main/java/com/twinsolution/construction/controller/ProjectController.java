package com.twinsolution.construction.controller;

import com.twinsolution.construction.dto.ProjectDto;
import com.twinsolution.construction.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectDto.Response> createProject(@RequestBody ProjectDto.Request request) {
        ProjectDto.Response response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProjectDto.Response>> getRecentProjects(
            @RequestParam(defaultValue = "5") int limit) {
        List<ProjectDto.Response> projects = projectService.getRecentProjects(limit);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDto.Response> getProject(@PathVariable Long projectId) {
        ProjectDto.Response project = projectService.getProjectById(projectId);
        return ResponseEntity.ok(project);
    }

    @PatchMapping("/{projectId}")
    public ResponseEntity<ProjectDto.Response> updateProject(
            @PathVariable Long projectId,
            @RequestBody ProjectDto.UpdateRequest request) {
        ProjectDto.Response response = projectService.updateProject(projectId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId) {
        projectService.deleteProject(projectId);
        return ResponseEntity.noContent().build();
    }
}
