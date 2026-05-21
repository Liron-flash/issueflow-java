package com.att.tdp.issueflow.controllers;


import jakarta.validation.Valid;
import com.att.tdp.issueflow.entities.Comment;
import com.att.tdp.issueflow.services.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets/{ticketId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<List<Comment>> getCommentsByTicket(@PathVariable Long ticketId) {
        try {
            return ResponseEntity.ok(commentService.getCommentsByTicketId(ticketId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<Comment> addComment(
            @PathVariable Long ticketId,
            @Valid @RequestBody Comment comment
    ) {
        try {
            return ResponseEntity.ok(commentService.addComment(ticketId, comment));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<Void> updateComment(
            @PathVariable Long ticketId,
            @PathVariable Long commentId,
            @Valid @RequestBody Comment comment
    ) {
        try {
            commentService.updateComment(ticketId, commentId, comment);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long ticketId,
            @PathVariable Long commentId
    ) {
        try {
            commentService.deleteComment(ticketId, commentId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
