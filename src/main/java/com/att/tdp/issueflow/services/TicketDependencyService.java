package com.att.tdp.issueflow.services;

import com.att.tdp.issueflow.entities.Ticket;
import com.att.tdp.issueflow.entities.TicketDependency;
import com.att.tdp.issueflow.repositories.TicketDependencyRepository;
import com.att.tdp.issueflow.repositories.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketDependencyService {

    private final TicketDependencyRepository ticketDependencyRepository;
    private final TicketRepository ticketRepository;
    private final AuditLogService auditLogService;

    public void addDependency(Long ticketId, Long blockedBy) {
        Ticket ticket = getExistingTicket(ticketId);
        Ticket blocker = getExistingTicket(blockedBy);

        if (ticketId.equals(blockedBy)) {
            throw new IllegalArgumentException("A ticket cannot depend on itself");
        }

        if (!ticket.getProjectId().equals(blocker.getProjectId())) {
            throw new IllegalArgumentException("Both tickets must belong to the same project");
        }

        if (ticketDependencyRepository.existsByTicketIdAndBlockerId(ticketId, blockedBy)) {
            return;
        }

        TicketDependency dependency = TicketDependency.builder()
                .ticketId(ticketId)
                .blockerId(blockedBy)
                .build();

        ticketDependencyRepository.save(dependency);
        auditLogService.logUserAction("ADD_DEPENDENCY", "TICKET", ticketId);
    }

    public List<Ticket> getDependencies(Long ticketId) {
        getExistingTicket(ticketId);

        return ticketDependencyRepository.findByTicketId(ticketId)
                .stream()
                .map(TicketDependency::getBlockerId)
                .map(this::getExistingTicket)
                .toList();
    }

    public void removeDependency(Long ticketId, Long blockerId) {
        getExistingTicket(ticketId);
        getExistingTicket(blockerId);

        TicketDependency dependency = ticketDependencyRepository
                .findByTicketIdAndBlockerId(ticketId, blockerId)
                .orElseThrow(() -> new RuntimeException("Dependency not found"));

        ticketDependencyRepository.delete(dependency);
        auditLogService.logUserAction("REMOVE_DEPENDENCY", "TICKET", ticketId);
    }

    private Ticket getExistingTicket(Long ticketId) {
        return ticketRepository.findByIdAndDeletedFalse(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket does not exist with id: " + ticketId));
    }
}