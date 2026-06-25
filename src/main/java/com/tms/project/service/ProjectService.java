package com.tms.project.service;

import com.tms.project.dto.ProjectRequest;
import com.tms.project.dto.ProjectResponse;
import com.tms.project.entity.Project;
import com.tms.project.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public List<ProjectResponse> getProjects() {
        return projectRepository.findAllByOrderByCreatedAtAsc().stream()
                .map(ProjectResponse::from).toList();
    }

    public ProjectResponse getProject(Long id) {
        return ProjectResponse.from(findById(id));
    }

    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        Project project = new Project(request.name(), request.description(), request.owner());
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        Project project = findById(id);
        project.update(request.name(), request.description(), request.owner());
        return ProjectResponse.from(project);
    }

    @Transactional
    public void deleteProject(Long id) {
        projectRepository.delete(findById(id));
    }

    private Project findById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found. id=" + id));
    }
}
