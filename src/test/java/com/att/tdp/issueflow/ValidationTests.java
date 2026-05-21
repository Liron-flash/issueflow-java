package com.att.tdp.issueflow;

import com.att.tdp.issueflow.entities.Project;
import com.att.tdp.issueflow.entities.Role;
import com.att.tdp.issueflow.entities.Ticket;
import com.att.tdp.issueflow.entities.TicketPriority;
import com.att.tdp.issueflow.entities.TicketStatus;
import com.att.tdp.issueflow.entities.TicketType;
import com.att.tdp.issueflow.entities.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationTests {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void projectWithoutNameFailsValidation() {
        Project project = Project.builder()
                .description("Missing name")
                .ownerId(1L)
                .build();

        Set<ConstraintViolation<Project>> violations = validator.validate(project);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("name")));
    }

    @Test
    void ticketWithoutTitleFailsValidation() {
        Ticket ticket = Ticket.builder()
                .description("Missing title")
                .status(TicketStatus.TODO)
                .priority(TicketPriority.HIGH)
                .type(TicketType.BUG)
                .projectId(1L)
                .build();

        Set<ConstraintViolation<Ticket>> violations = validator.validate(ticket);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("title")));
    }

    @Test
    void userWithInvalidEmailFailsValidation() {
        User user = User.builder()
                .username("validuser")
                .email("not-an-email")
                .fullName("Valid User")
                .role(Role.DEVELOPER)
                .password("secret123")
                .build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("email")));
    }

    @Test
    void validTicketPassesValidation() {
        Ticket ticket = Ticket.builder()
                .title("Valid ticket")
                .description("Valid description")
                .status(TicketStatus.TODO)
                .priority(TicketPriority.MEDIUM)
                .type(TicketType.FEATURE)
                .projectId(1L)
                .build();

        Set<ConstraintViolation<Ticket>> violations = validator.validate(ticket);

        assertTrue(violations.isEmpty());
    }
}