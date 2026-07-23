package etiya.omniAutomation.controller;

import etiya.omniAutomation.business.dto.ProjectDto;
import etiya.omniAutomation.common.PermissionConstants;
import etiya.omniAutomation.config.security.AuthorizationUserDetails;
import etiya.omniAutomation.config.security.JwtUtils;
import etiya.omniAutomation.results.Result;
import etiya.omniAutomation.service.ProjectService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;
    private final JwtUtils jwtUtils;

    @GetMapping("/all")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PROJECT_VIEW + "')")
    public ResponseEntity<List<ProjectDto>> getAllProjects(HttpServletRequest request) {
        try {
            CurrentUserToken currentUser = getCurrentUserToken(request);
            List<ProjectDto> projects = projectService.getProjectsForCurrentUser(currentUser.userId());
            log.info("Fetched {} projects for user {}", projects.size(), currentUser.username());
            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            log.error("Error fetching all projects", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{projectId}")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PROJECT_VIEW + "')")
    public ResponseEntity<ProjectDto> getProjectById(@PathVariable Long projectId, HttpServletRequest request) {
        try {
            CurrentUserToken currentUser = getCurrentUserToken(request);
            if (!projectService.canCurrentUserAccessProject(currentUser.userId(), projectId)) {
                return ResponseEntity.status(403).build();
            }
            ProjectDto project = projectService.getProject(projectId);
            if (project == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(project);
        } catch (Exception e) {
            log.error("Error fetching project by id: {}", projectId, e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/short-code/{shortCode}")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PROJECT_VIEW + "')")
    public ResponseEntity<ProjectDto> getProjectByShortCode(@PathVariable String shortCode, HttpServletRequest request) {
        try {
            ProjectDto project = projectService.getProject(shortCode);
            if (project == null) {
                return ResponseEntity.notFound().build();
            }
            CurrentUserToken currentUser = getCurrentUserToken(request);
            if (!projectService.canCurrentUserAccessProject(currentUser.userId(), project.getProjectId())) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.ok(project);
        } catch (Exception e) {
            log.error("Error fetching project by short code: {}", shortCode, e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PROJECT_VIEW + "')")
    public ResponseEntity<List<ProjectDto>> getUserProjects(@PathVariable Long userId) {
        try {
            List<ProjectDto> projects = projectService.getUserProjects(userId);
            log.info("Fetched {} projects for user {}", projects.size(), userId);
            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            log.error("Error fetching projects for user: {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/my-projects")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProjectDto>> getMyProjects() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }
            
            if (authentication.getPrincipal() instanceof AuthorizationUserDetails userDetails) {
                List<ProjectDto> projects = projectService.getUserProjects(userDetails.principalId());
                log.info("Fetched {} projects for current user", projects.size());
                return ResponseEntity.ok(projects);
            }
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            log.error("Error fetching projects for current user", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/save")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PROJECT_CREATE + "')")
    public ResponseEntity<Result> saveProject(@RequestBody ProjectDto projectDto) {
        try {
            Result result = projectService.saveProject(projectDto);
            log.info("Project saved: {}", projectDto.getName());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error saving project", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/update")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PROJECT_UPDATE + "')")
    public ResponseEntity<Result> updateProject(@RequestBody ProjectDto projectDto) {
        try {
            Result result = projectService.updateProject(projectDto);
            log.info("Project updated: {}", projectDto.getName());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error updating project", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{projectId}")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PROJECT_DELETE + "')")
    public ResponseEntity<Result> deleteProject(@PathVariable Long projectId) {
        try {
            Result result = projectService.deleteProject(projectId);
            log.info("Project deleted: {}", projectId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error deleting project: {}", projectId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Project service is running");
    }

    private CurrentUserToken getCurrentUserToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization token is required");
        }
        String token = authHeader.substring(7);
        Claims claims = jwtUtils.parseClaims(token);
        String username = claims.getSubject();
        String authType = claims.get("authType") != null ? claims.get("authType").toString() : "local";
        Object userIdRaw = claims.get("userId");
        Long userId = userIdRaw instanceof Number ? ((Number) userIdRaw).longValue() : null;
        return new CurrentUserToken(username, authType, userId);
    }

    private record CurrentUserToken(String username, String authType, Long userId) { }
}
