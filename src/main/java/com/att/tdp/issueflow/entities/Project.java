package com.att.tdp.issueflow.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Project name is required")
    @Size(max = 200, message = "Project name must be at most 200 characters")
    @Column(nullable = false)
    private String name;

    @Size(max = 5000, message = "Project description must be at most 5000 characters")
    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Owner id is required")
    @Positive(message = "Owner id must be positive")
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;
}
