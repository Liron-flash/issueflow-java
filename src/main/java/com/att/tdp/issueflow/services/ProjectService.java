package com.att.tdp.issueflow.services;

import com.att.tdp.issueflow.entities.Project;
import com.att.tdp.issueflow.entities.Role;
import com.att.tdp.issueflow.entities.TicketStatus;
import com.att.tdp.issueflow.entities.User;
import com.att.tdp.issueflow.repositories.ProjectRepository;
import com.att.tdp.issueflow.repositories.TicketRepository;
import com.att.tdp.issueflow.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final AuditLogService auditLogService;

    public Project createProject(Project project) {
        validateProjectForCreation(project);

        if (!userRepository.existsById(project.getOwnerId())) {
            throw new IllegalArgumentException("Owner user does not exist");
        }

        project.setDeleted(false);

        Project savedProject = projectRepository.save(project);
        auditLogService.logUserAction("CREATE", "PROJECT", savedProject.getId());

        return savedProject;
    }

    public List<Project> getAllProjects() {
        return projectRepository.findByDeletedFalse();
    }

    public List<Project> getDeletedProjects() {
        return projectRepository.findByDeletedTrue();
    }

    public List<Map<String, Object>> getProjectWorkload(Long projectId) {
        if (!projectRepository.existsByIdAndDeletedFalse(projectId)) {
            throw new IllegalArgumentException("Project not found with id: " + projectId);
        }

        return userRepository.findByRoleOrderByIdAsc(Role.DEVELOPER)
                .stream()
                .map(user -> buildWorkloadRecord(projectId, user))
                .sorted(
                        Comparator
                                .comparingLong((Map<String, Object> record) -> (Long) record.get("openTicketCount"))
                                .thenComparingLong(record -> (Long) record.get("userId"))
                )
                .toList();
    }

    public Project getProjectById(Long id) {
        return projectRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found with id: " + id));
    }

    @Transactional
    public Project updateProject(Long id, Project updatedProject) {
        Project existingProject = getProjectById(id);

        if (updatedProject.getName() != null && !updatedProject.getName().isBlank()) {
            existingProject.setName(updatedProject.getName());
        }

        if (updatedProject.getDescription() != null) {
            existingProject.setDescription(updatedProject.getDescription());
        }

        Project savedProject = projectRepository.save(existingProject);
        auditLogService.logUserAction("UPDATE", "PROJECT", savedProject.getId());

        return savedProject;
    }

    @Transactional
    public void deleteProject(Long id) {
        Project existingProject = getProjectById(id);

        existingProject.setDeleted(true);
        projectRepository.save(existingProject);

        auditLogService.logUserAction("DELETE", "PROJECT", id);
    }

    @Transactional
    public void restoreProject(Long id) {
        validateCurrentUserIsAdmin();

        Project project = projectRepository.findByIdAndDeletedTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Deleted project not found with id: " + id));

        project.setDeleted(false);
        projectRepository.save(project);

        auditLogService.logUserAction("RESTORE", "PROJECT", id);
    }

    private Map<String, Object> buildWorkloadRecord(Long projectId, User user) {
        long openTicketCount = ticketRepository.countByProjectIdAndAssigneeIdAndDeletedFalseAndStatusNot(
                projectId,
                user.getId(),
                TicketStatus.DONE
        );

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("userId", user.getId());
        record.put("username", user.getUsername());
        record.put("openTicketCount", openTicketCount);

        return record;
    }

    private void validateProjectForCreation(Project project) {
        if (project.getName() == null || project.getName().isBlank()) {
            throw new IllegalArgumentException("Project name is required");
        }

        if (project.getOwnerId() == null) {
            throw new IllegalArgumentException("Project ownerId is required");
        }
    }

    private void validateCurrentUserIsAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        if (!isAdmin) {
            throw new AccessDeniedException("Only ADMIN users can restore projects");
        }
    }
}