package com.att.tdp.issueflow.config;

import com.att.tdp.issueflow.entities.Project;
import com.att.tdp.issueflow.entities.Role;
import com.att.tdp.issueflow.entities.User;
import com.att.tdp.issueflow.repositories.ProjectRepository;
import com.att.tdp.issueflow.repositories.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DataInitializer(UserRepository userRepository, ProjectRepository projectRepository) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        User developer = ensureUser(
                "jdoe",
                "jdoe@example.com",
                "John Doe",
                Role.DEVELOPER,
                "secret"
        );

        User admin = ensureUser(
                "admin2",
                "admin2@example.com",
                "Admin User",
                Role.ADMIN,
                "adminpass"
        );

        ensureUser(
                "test_dev",
                "test_dev@example.com",
                "Test Developer",
                Role.DEVELOPER,
                "secret"
        );

        ensureProject("Default Project", admin.getId());

        if (developer.getId() != null) {
            ensureProject("Developer Project", developer.getId());
        }
    }

    private User ensureUser(
            String username,
            String email,
            String fullName,
            Role role,
            String rawPassword
    ) {
        return userRepository.findAll()
                .stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst()
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .username(username)
                                .email(email)
                                .fullName(fullName)
                                .role(role)
                                .password(passwordEncoder.encode(rawPassword))
                                .build()
                ));
    }

    private void ensureProject(String name, Long ownerId) {
        boolean exists = projectRepository.findAll()
                .stream()
                .anyMatch(project -> project.getName().equals(name));

        if (!exists) {
            projectRepository.save(
                    Project.builder()
                            .name(name)
                            .description("Automatically created default project")
                            .ownerId(ownerId)
                            .build()
            );
        }
    }
}