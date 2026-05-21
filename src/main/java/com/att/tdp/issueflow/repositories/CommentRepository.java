package com.att.tdp.issueflow.repositories;

import com.att.tdp.issueflow.entities.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByTicketIdOrderByIdAsc(Long ticketId);

    Optional<Comment> findByIdAndTicketId(Long id, Long ticketId);

    @Query("select c from Comment c join c.mentionedUsers u where u.id = :userId order by c.id desc")
    List<Comment> findCommentsMentioningUserOrderByNewestFirst(@Param("userId") Long userId);
}