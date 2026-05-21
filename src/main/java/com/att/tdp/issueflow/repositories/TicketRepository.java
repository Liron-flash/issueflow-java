package com.att.tdp.issueflow.repositories;

import com.att.tdp.issueflow.entities.Ticket;
import com.att.tdp.issueflow.entities.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByProjectIdAndDeletedFalse(Long projectId);

    List<Ticket> findByProjectIdAndDeletedTrue(Long projectId);

    Optional<Ticket> findByIdAndDeletedFalse(Long id);

    Optional<Ticket> findByIdAndDeletedTrue(Long id);

    List<Ticket> findByAssigneeIdAndDeletedFalse(Long assigneeId);

    List<Ticket> findByDeletedFalseAndDueDateBeforeAndStatusNot(
            LocalDateTime currentTime,
            TicketStatus status
    );

    long countByProjectIdAndAssigneeIdAndDeletedFalseAndStatusNot(
            Long projectId,
            Long assigneeId,
            TicketStatus status
    );
}