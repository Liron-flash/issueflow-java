package com.att.tdp.issueflow.services;

import com.att.tdp.issueflow.entities.Role;
import com.att.tdp.issueflow.entities.Ticket;
import com.att.tdp.issueflow.entities.TicketDependency;
import com.att.tdp.issueflow.entities.TicketPriority;
import com.att.tdp.issueflow.entities.TicketStatus;
import com.att.tdp.issueflow.entities.TicketType;
import com.att.tdp.issueflow.entities.User;
import com.att.tdp.issueflow.repositories.ProjectRepository;
import com.att.tdp.issueflow.repositories.TicketDependencyRepository;
import com.att.tdp.issueflow.repositories.TicketRepository;
import com.att.tdp.issueflow.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final TicketDependencyRepository ticketDependencyRepository;

    public Ticket createTicket(Ticket ticket) {
        validateTicketForCreation(ticket);

        if (!projectRepository.existsByIdAndDeletedFalse(ticket.getProjectId())) {
            throw new IllegalArgumentException("Project does not exist");
        }

        boolean autoAssigned = false;

        if (ticket.getAssigneeId() != null) {
            if (!userRepository.existsById(ticket.getAssigneeId())) {
                throw new IllegalArgumentException("Assignee user does not exist");
            }
        } else {
            autoAssigned = autoAssignTicketIfPossible(ticket);
        }

        ticket.setDeleted(false);
        ticket.setOverdue(false);

        Ticket savedTicket = ticketRepository.save(ticket);
        auditLogService.logUserAction("CREATE", "TICKET", savedTicket.getId());

        if (autoAssigned) {
            auditLogService.logSystemAction("AUTO_ASSIGN", "TICKET", savedTicket.getId());
        }

        return savedTicket;
    }

    public List<Ticket> getTicketsByProjectId(Long projectId) {
        if (!projectRepository.existsByIdAndDeletedFalse(projectId)) {
            throw new IllegalArgumentException("Project does not exist");
        }

        return ticketRepository.findByProjectIdAndDeletedFalse(projectId);
    }

    public List<Ticket> getDeletedTicketsByProjectId(Long projectId) {
        if (!projectRepository.existsByIdAndDeletedFalse(projectId)) {
            throw new IllegalArgumentException("Project does not exist");
        }

        return ticketRepository.findByProjectIdAndDeletedTrue(projectId);
    }

    public String exportTicketsToCsv(Long projectId) {
        if (!projectRepository.existsByIdAndDeletedFalse(projectId)) {
            throw new IllegalArgumentException("Project does not exist");
        }

        List<Ticket> tickets = ticketRepository.findByProjectIdAndDeletedFalse(projectId);

        try (
                StringWriter stringWriter = new StringWriter();
                CSVPrinter csvPrinter = new CSVPrinter(
                        stringWriter,
                        CSVFormat.DEFAULT.builder()
                                .setHeader("id", "title", "description", "status", "priority", "type", "assigneeId")
                                .build()
                )
        ) {
            for (Ticket ticket : tickets) {
                csvPrinter.printRecord(
                        ticket.getId(),
                        ticket.getTitle(),
                        ticket.getDescription(),
                        ticket.getStatus(),
                        ticket.getPriority(),
                        ticket.getType(),
                        ticket.getAssigneeId()
                );
            }

            csvPrinter.flush();
            return stringWriter.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export tickets to CSV", e);
        }
    }

    public Map<String, Object> importTicketsFromCsv(Long projectId, MultipartFile file) {
        if (!projectRepository.existsByIdAndDeletedFalse(projectId)) {
            throw new IllegalArgumentException("Project does not exist");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is required");
        }

        int created = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)
                );
                CSVParser csvParser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setTrim(true)
                        .build()
                        .parse(reader)
        ) {
            for (CSVRecord record : csvParser) {
                long lineNumber = record.getRecordNumber() + 1;

                try {
                    Ticket ticket = buildTicketFromCsvRecord(projectId, record);
                    createTicket(ticket);
                    created++;
                } catch (RuntimeException e) {
                    failed++;
                    errors.add("Line " + lineNumber + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read CSV file", e);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);
        result.put("failed", failed);
        result.put("errors", errors);
        return result;
    }

    public Ticket getTicketById(Long id) {
        return ticketRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + id));
    }

    @Transactional
    public Ticket updateTicket(Long id, Ticket updatedTicket) {
        Ticket existingTicket = getTicketById(id);

        if (existingTicket.getStatus() == TicketStatus.DONE) {
            throw new IllegalStateException("Cannot update a ticket that is DONE");
        }

        if (updatedTicket.getStatus() != null && updatedTicket.getStatus() != existingTicket.getStatus()) {
            validateStatusTransition(existingTicket.getStatus(), updatedTicket.getStatus());

            if (updatedTicket.getStatus() == TicketStatus.DONE) {
                validateNoUnresolvedBlockers(existingTicket.getId());
            }

            existingTicket.setStatus(updatedTicket.getStatus());
        }

        if (updatedTicket.getTitle() != null && !updatedTicket.getTitle().isBlank()) {
            existingTicket.setTitle(updatedTicket.getTitle());
        }

        if (updatedTicket.getDescription() != null) {
            existingTicket.setDescription(updatedTicket.getDescription());
        }

        if (updatedTicket.getPriority() != null) {
            existingTicket.setPriority(updatedTicket.getPriority());
            existingTicket.setOverdue(false);
        }

        if (updatedTicket.getAssigneeId() != null) {
            if (!userRepository.existsById(updatedTicket.getAssigneeId())) {
                throw new IllegalArgumentException("Assignee user does not exist");
            }

            existingTicket.setAssigneeId(updatedTicket.getAssigneeId());
        }

        if (updatedTicket.getDueDate() != null) {
            existingTicket.setDueDate(updatedTicket.getDueDate());
        }

        try {
            Ticket savedTicket = ticketRepository.save(existingTicket);
            auditLogService.logUserAction("UPDATE", "TICKET", savedTicket.getId());
            return savedTicket;
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException("Ticket was updated by another user at the same time");
        }
    }

    @Transactional
    public void deleteTicket(Long id) {
        Ticket ticket = getTicketById(id);
        ticket.setDeleted(true);
        ticketRepository.save(ticket);
        auditLogService.logUserAction("DELETE", "TICKET", id);
    }

    @Transactional
    public void restoreTicket(Long id) {
        validateCurrentUserIsAdmin();

        Ticket ticket = ticketRepository.findByIdAndDeletedTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Deleted ticket not found with id: " + id));

        ticket.setDeleted(false);
        ticketRepository.save(ticket);
        auditLogService.logUserAction("RESTORE", "TICKET", id);
    }

    private boolean autoAssignTicketIfPossible(Ticket ticket) {
        List<User> developers = userRepository.findByRoleOrderByIdAsc(Role.DEVELOPER);

        if (developers.isEmpty()) {
            return false;
        }

        User selectedDeveloper = developers.stream()
                .min(
                        Comparator
                                .comparingLong((User user) -> countOpenTicketsForUser(ticket.getProjectId(), user.getId()))
                                .thenComparingLong(User::getId)
                )
                .orElse(null);

        if (selectedDeveloper == null) {
            return false;
        }

        ticket.setAssigneeId(selectedDeveloper.getId());
        return true;
    }

    private long countOpenTicketsForUser(Long projectId, Long userId) {
        return ticketRepository.countByProjectIdAndAssigneeIdAndDeletedFalseAndStatusNot(
                projectId,
                userId,
                TicketStatus.DONE
        );
    }

    private Ticket buildTicketFromCsvRecord(Long projectId, CSVRecord record) {
        String title = getRequiredCsvValue(record, "title");
        String description = getOptionalCsvValue(record, "description");
        TicketStatus status = parseEnum(TicketStatus.class, getRequiredCsvValue(record, "status"), "status");
        TicketPriority priority = parseEnum(TicketPriority.class, getRequiredCsvValue(record, "priority"), "priority");
        TicketType type = parseEnum(TicketType.class, getRequiredCsvValue(record, "type"), "type");
        Long assigneeId = parseOptionalLong(getOptionalCsvValue(record, "assigneeId"), "assigneeId");

        Ticket ticket = new Ticket();
        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setStatus(status);
        ticket.setPriority(priority);
        ticket.setType(type);
        ticket.setProjectId(projectId);
        ticket.setAssigneeId(assigneeId);

        return ticket;
    }

    private String getRequiredCsvValue(CSVRecord record, String columnName) {
        String value = getOptionalCsvValue(record, columnName);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required column value: " + columnName);
        }

        return value;
    }

    private String getOptionalCsvValue(CSVRecord record, String columnName) {
        for (Map.Entry<String, String> entry : record.toMap().entrySet()) {
            String actualColumnName = entry.getKey();

            if (actualColumnName != null) {
                actualColumnName = actualColumnName.replace("\uFEFF", "").trim();
            }

            if (columnName.equals(actualColumnName)) {
                String value = entry.getValue();
                return value == null ? null : value.trim();
            }
        }

        return null;
    }

    private Long parseOptionalLong(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + fieldName + ": " + value);
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String fieldName) {
        try {
            return Enum.valueOf(enumClass, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid " + fieldName + ": " + value);
        }
    }

    private void validateTicketForCreation(Ticket ticket) {
        if (ticket.getTitle() == null || ticket.getTitle().isBlank()) {
            throw new IllegalArgumentException("Ticket title is required");
        }

        if (ticket.getStatus() == null) {
            throw new IllegalArgumentException("Ticket status is required");
        }

        if (ticket.getPriority() == null) {
            throw new IllegalArgumentException("Ticket priority is required");
        }

        if (ticket.getType() == null) {
            throw new IllegalArgumentException("Ticket type is required");
        }

        if (ticket.getProjectId() == null) {
            throw new IllegalArgumentException("Ticket projectId is required");
        }
    }

    private void validateStatusTransition(TicketStatus currentStatus, TicketStatus nextStatus) {
        if (nextStatus.ordinal() < currentStatus.ordinal()) {
            throw new IllegalStateException("Backward status transitions are not allowed");
        }

        if (nextStatus.ordinal() > currentStatus.ordinal() + 1) {
            throw new IllegalStateException("Status can only move one step forward");
        }
    }

    private void validateNoUnresolvedBlockers(Long ticketId) {
        List<TicketDependency> dependencies = ticketDependencyRepository.findByTicketId(ticketId);

        for (TicketDependency dependency : dependencies) {
            Ticket blocker = ticketRepository.findByIdAndDeletedFalse(dependency.getBlockerId())
                    .orElseThrow(() -> new IllegalStateException("Blocking ticket does not exist"));

            if (blocker.getStatus() != TicketStatus.DONE) {
                throw new IllegalStateException("Ticket cannot be marked DONE while it has unresolved blockers");
            }
        }
    }

    private void validateCurrentUserIsAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication is required");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        if (!isAdmin) {
            throw new AccessDeniedException("Only ADMIN users can restore tickets");
        }
    }
}