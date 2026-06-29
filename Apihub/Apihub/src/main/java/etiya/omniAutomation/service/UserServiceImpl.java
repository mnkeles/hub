package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.ProjectDto;
import etiya.omniAutomation.entity.ProjectEntity;
import etiya.omniAutomation.entity.UserEntity;
import etiya.omniAutomation.mappers.ProjectMapper;
import etiya.omniAutomation.repository.UserProjectRelationRepository;
import etiya.omniAutomation.repository.UserRepository;
import etiya.omniAutomation.results.*;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserDetailsService, UserDetailsManager {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserProjectRelationRepository userProjectRelationRepository;

    public List<UserEntity> getAll() {
        return this.userRepository.findAll();
    }

    public Result save(UserEntity userEntity) {
        try {
            String password = userEntity.getPassword();
            String email = userEntity.getEmail();
            Optional<UserEntity> userResult = getUserByEmail(email);
            if (userResult.isPresent()) {
                throw new IllegalStateException("User with email " + email + " already exists");
            }

            if (password != null) {
                password = passwordEncoder.encode(password);
                userEntity.setPassword(password);
            }
            this.userRepository.save(userEntity);
            return new SuccessResult();
        } catch (Exception e) {
            return new ErrorResult(e.getMessage());
        }
    }

    public String getUserPasswordByEmail(String email) {
        return this.userRepository.getUserPasswordByEmail(email);
    }

    public Optional<UserEntity> getUserByEmail(String email) {
        return this.userRepository.findByEmailAndEnabled(email, 1);
    }

    public Optional<UserEntity> getUserByAnyEmail(String email) {
        return Optional.ofNullable(this.userRepository.findByEmail(email));
    }

    public Result update(UserEntity userEntity) {
        try {
            this.userRepository.save(userEntity);
            return new SuccessResult();
        } catch (Exception e) {
            return new ErrorResult(e.getMessage());
        }
    }

    public DataResult<List<UserEntity>> getAllUser() {
        try{
            List<UserEntity> userEntities = this.userRepository.getUserIsEnabled();
            return new SuccessDataResult(userEntities);
        }catch (Exception e){
            return new ErrorDataResult(null,e.getMessage());
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity byEmail = userRepository.findByEmail(username);
        if (Objects.isNull(byEmail)) {
            throw new UsernameNotFoundException("Böyle bir kullanıcı bulunamadı");
        }

        List<SimpleGrantedAuthority> roleUser = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        return new User(username, byEmail.getPassword(), roleUser);
    }

    @Override
    public void createUser(UserDetails user) {
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(user.getUsername());
        userEntity.setPassword(user.getPassword());
        this.save(userEntity);
    }

    @Override
    public void updateUser(UserDetails user) {

    }

    @Override
    public void deleteUser(String username) {

    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {

    }

    @Override
    public boolean userExists(String username) {
        return true;
    }

    public List<ProjectDto> getUserProjects(Long userId) {
        List<ProjectEntity> userProjects = this.userProjectRelationRepository.findUserProjects(userId);
        return ProjectMapper.INSTANCE.toDtoList(userProjects);
    }

//    public Long getLoggedInUserId() {
//        String authenticatedUser = this.securityService.getAuthenticatedUser();
//        return this.userRepository.findUserIdWithEmail(authenticatedUser);
//    }
}
