package com.att.tdp.issueflow.repositories;

import com.att.tdp.issueflow.entities.TicketDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketDependencyRepository extends JpaRepository<TicketDependency, Long> {

    boolean existsByTicketIdAndBlockerId(Long ticketId, Long blockerId);

    Optional<TicketDependency> findByTicketIdAndBlockerId(Long ticketId, Long blockerId);

    List<TicketDependency> findByTicketId(Long ticketId);
}