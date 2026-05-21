package com.att.tdp.issueflow.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Ticket title is required")
    @Size(max = 200, message = "Ticket title must be at most 200 characters")
    @Column(nullable = false)
    private String title;

    @Size(max = 5000, message = "Ticket description must be at most 5000 characters")
    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Ticket status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @NotNull(message = "Ticket priority is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketPriority priority;

    @NotNull(message = "Ticket type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketType type;

    @NotNull(message = "Project id is required")
    @Positive(message = "Project id must be positive")
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Positive(message = "Assignee id must be positive")
    @Column(name = "assignee_id")
    private Long assigneeId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "is_overdue", nullable = false)
    @Builder.Default
    private boolean overdue = false;

    @JsonIgnore
    @Version
    private Long version;
}
