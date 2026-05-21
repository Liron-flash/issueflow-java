package com.att.tdp.issueflow.entities;

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

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Ticket id is required")
    @Positive(message = "Ticket id must be positive")
    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @NotNull(message = "Author id is required")
    @Positive(message = "Author id must be positive")
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @NotBlank(message = "Comment content is required")
    @Size(max = 5000, message = "Comment content must be at most 5000 characters")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @ManyToMany
    @JoinTable(
            name = "comment_mentions",
            joinColumns = @JoinColumn(name = "comment_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<User> mentionedUsers = new LinkedHashSet<>();

    @JsonIgnore
    @Version
    private Long version;
}
