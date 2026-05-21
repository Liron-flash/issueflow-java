package com.att.tdp.issueflow.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "ticket_dependencies",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"ticket_id", "blocker_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "blocker_id", nullable = false)
    private Long blockerId;
}