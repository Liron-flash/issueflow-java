package com.att.tdp.issueflow.services;

import com.att.tdp.issueflow.entities.Ticket;
import com.att.tdp.issueflow.entities.TicketPriority;
import com.att.tdp.issueflow.entities.TicketStatus;
import com.att.tdp.issueflow.repositories.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketEscalationService {

    private final TicketRepository ticketRepository;
    private final AuditLogService auditLogService;

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void escalateOverdueTickets() {
        List<Ticket> overdueTickets = ticketRepository.findByDeletedFalseAndDueDateBeforeAndStatusNot(
                LocalDateTime.now(),
                TicketStatus.DONE
        );

        for (Ticket ticket : overdueTickets) {
            boolean changed = escalateTicketIfNeeded(ticket);

            if (changed) {
                ticketRepository.save(ticket);
                auditLogService.logSystemAction("AUTO_ESCALATE", "TICKET", ticket.getId());
            }
        }
    }

    private boolean escalateTicketIfNeeded(Ticket ticket) {
        if (ticket.getDueDate() == null) {
            return false;
        }

        if (ticket.getPriority() == null) {
            return false;
        }

        if (ticket.getPriority() == TicketPriority.CRITICAL) {
            if (!ticket.isOverdue()) {
                ticket.setOverdue(true);
                return true;
            }

            return false;
        }

        ticket.setPriority(nextPriority(ticket.getPriority()));
        ticket.setOverdue(false);
        return true;
    }

    private TicketPriority nextPriority(TicketPriority currentPriority) {
        return switch (currentPriority) {
            case LOW -> TicketPriority.MEDIUM;
            case MEDIUM -> TicketPriority.HIGH;
            case HIGH -> TicketPriority.CRITICAL;
            case CRITICAL -> TicketPriority.CRITICAL;
        };
    }
}