package com.att.tdp.issueflow.controllers;


import jakarta.validation.Valid;
import com.att.tdp.issueflow.entities.Ticket;
import com.att.tdp.issueflow.services.TicketDependencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets/{ticketId}/dependencies")
@RequiredArgsConstructor
public class TicketDependencyController {

    private final TicketDependencyService ticketDependencyService;

    @PostMapping
    public ResponseEntity<Void> addDependency(
            @PathVariable Long ticketId,
            @Valid @RequestBody AddDependencyRequest request
    ) {
        try {
            ticketDependencyService.addDependency(ticketId, request.blockedBy());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Ticket>> getDependencies(@PathVariable Long ticketId) {
        try {
            return ResponseEntity.ok(ticketDependencyService.getDependencies(ticketId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{blockerId}")
    public ResponseEntity<Void> removeDependency(
            @PathVariable Long ticketId,
            @PathVariable Long blockerId
    ) {
        try {
            ticketDependencyService.removeDependency(ticketId, blockerId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    public record AddDependencyRequest(Long blockedBy) {
    }
}
