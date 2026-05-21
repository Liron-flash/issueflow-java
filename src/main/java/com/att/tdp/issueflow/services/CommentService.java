package com.att.tdp.issueflow.services;

import com.att.tdp.issueflow.entities.Comment;
import com.att.tdp.issueflow.entities.User;
import com.att.tdp.issueflow.repositories.CommentRepository;
import com.att.tdp.issueflow.repositories.TicketRepository;
import com.att.tdp.issueflow.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CommentService {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([A-Za-z0-9_.-]+)");

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public Comment addComment(Long ticketId, Comment comment) {
        validateTicketExists(ticketId);
        validateCommentForCreation(comment);

        if (!userRepository.existsById(comment.getAuthorId())) {
            throw new IllegalArgumentException("Author user does not exist");
        }

        comment.setTicketId(ticketId);
        comment.setMentionedUsers(extractMentionedUsers(comment.getContent()));

        Comment savedComment = commentRepository.save(comment);
        auditLogService.logUserAction("CREATE", "COMMENT", savedComment.getId());

        return savedComment;
    }

    public List<Comment> getCommentsByTicketId(Long ticketId) {
        validateTicketExists(ticketId);
        return commentRepository.findByTicketIdOrderByIdAsc(ticketId);
    }

    public List<Comment> getCommentsMentioningUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User does not exist");
        }

        return commentRepository.findCommentsMentioningUserOrderByNewestFirst(userId);
    }

    @Transactional
    public Comment updateComment(Long ticketId, Long commentId, Comment updatedComment) {
        validateTicketExists(ticketId);

        Comment existingComment = commentRepository.findByIdAndTicketId(commentId, ticketId)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));

        if (updatedComment.getContent() == null || updatedComment.getContent().isBlank()) {
            throw new IllegalArgumentException("Comment content is required");
        }

        existingComment.setContent(updatedComment.getContent());
        existingComment.getMentionedUsers().clear();
        existingComment.getMentionedUsers().addAll(extractMentionedUsers(updatedComment.getContent()));

        try {
            Comment savedComment = commentRepository.saveAndFlush(existingComment);
            auditLogService.logUserAction("UPDATE", "COMMENT", savedComment.getId());
            return savedComment;
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException("Comment was updated by another user at the same time");
        }
    }

    @Transactional
    public void deleteComment(Long ticketId, Long commentId) {
        validateTicketExists(ticketId);

        Comment existingComment = commentRepository.findByIdAndTicketId(commentId, ticketId)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));

        commentRepository.delete(existingComment);
        auditLogService.logUserAction("DELETE", "COMMENT", commentId);
    }

    private Set<User> extractMentionedUsers(String content) {
        Set<User> mentionedUsers = new LinkedHashSet<>();

        if (content == null || content.isBlank()) {
            return mentionedUsers;
        }

        Matcher matcher = MENTION_PATTERN.matcher(content);

        while (matcher.find()) {
            String username = matcher.group(1);

            userRepository.findByUsernameIgnoreCase(username)
                    .ifPresent(mentionedUsers::add);
        }

        return mentionedUsers;
    }

    private void validateTicketExists(Long ticketId) {
        if (!ticketRepository.findByIdAndDeletedFalse(ticketId).isPresent()) {
            throw new IllegalArgumentException("Ticket does not exist");
        }
    }

    private void validateCommentForCreation(Comment comment) {
        if (comment.getAuthorId() == null) {
            throw new IllegalArgumentException("Comment authorId is required");
        }

        if (comment.getContent() == null || comment.getContent().isBlank()) {
            throw new IllegalArgumentException("Comment content is required");
        }
    }
}