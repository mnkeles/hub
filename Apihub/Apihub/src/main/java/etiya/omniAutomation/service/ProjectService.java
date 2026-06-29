package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.ProjectDto;
import etiya.omniAutomation.entity.LdapUserEntity;
import etiya.omniAutomation.entity.ProjectEntity;
import etiya.omniAutomation.entity.UserEntity;
import etiya.omniAutomation.mappers.ProjectMapper;
import etiya.omniAutomation.repository.ProjectRepository;
import etiya.omniAutomation.repository.UserProjectRelationRepository;
import etiya.omniAutomation.results.Result;
import etiya.omniAutomation.results.SuccessResult;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserProjectRelationRepository userProjectRelationRepository;
    private final UserServiceImpl userService;
    private final LdapUserService ldapUserService;

    @Cacheable(value = "project", key = "#shortCode", condition = "#shortCode != null")
    public ProjectDto getProject(String shortCode) {
        if (shortCode == null) {
            return null;
        }
        ProjectEntity byShortCode = this.projectRepository.findByShortCode(shortCode);
        if (byShortCode == null) {
            return null;
        }
        return ProjectMapper.INSTANCE.toDto(byShortCode);
    }

    @Cacheable(value = "project", key = "T(java.lang.String).valueOf(#projectId)")
    public ProjectDto getProject(Long projectId) {
        return this.projectRepository.findById(projectId)
                .map(ProjectMapper.INSTANCE::toDto)
                .orElse(null);
    }

    public List<ProjectDto> getAllProjects() {
        return this.projectRepository.findAll()
                .stream()
                .map(ProjectMapper.INSTANCE::toDto)
                .collect(Collectors.toList());
    }

    public List<ProjectDto> getProjectsForCurrentUser(String username, String authType) {
        Long projectId = getCurrentUserProjectId(username, authType);
        if (projectId == null) {
            return getAllProjects();
        }
        return this.projectRepository.findAllByProjectId(projectId)
                .stream()
                .map(ProjectMapper.INSTANCE::toDto)
                .collect(Collectors.toList());
    }

    public boolean canCurrentUserAccessProject(String username, String authType, Long projectId) {
        Long currentUserProjectId = getCurrentUserProjectId(username, authType);
        return currentUserProjectId == null || currentUserProjectId.equals(projectId);
    }

    private Long getCurrentUserProjectId(String username, String authType) {
        if ("ldap".equalsIgnoreCase(authType)) {
            LdapUserEntity ldapUser = ldapUserService.getActiveLdapUserOrThrow(username);
            return ldapUser.getProjectId();
        }

        Optional<UserEntity> localUser = userService.getUserByAnyEmail(username);
        if (localUser.isPresent()) {
            return localUser.get().getProjectId();
        }

        return ldapUserService.getActiveLdapUserOrThrow(username).getProjectId();
    }

    public List<ProjectDto> getUserProjects(Long userId) {
        List<ProjectEntity> userProjects = this.userProjectRelationRepository.findUserProjects(userId);
        return userProjects.stream()
                .map(ProjectMapper.INSTANCE::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "project", allEntries = true)
    public Result saveProject(ProjectDto projectDto) {
        ProjectEntity entity = ProjectMapper.INSTANCE.toEntity(projectDto);
        this.projectRepository.save(entity);
        return new SuccessResult();
    }

    @Transactional
    @CacheEvict(value = "project", allEntries = true)
    public Result updateProject(ProjectDto projectDto) {
        ProjectEntity entity = ProjectMapper.INSTANCE.toEntity(projectDto);
        this.projectRepository.save(entity);
        return new SuccessResult();
    }

    @Transactional
    @CacheEvict(value = "project", allEntries = true)
    public Result deleteProject(Long projectId) {
        this.projectRepository.deleteById(projectId);
        return new SuccessResult();
    }

}
