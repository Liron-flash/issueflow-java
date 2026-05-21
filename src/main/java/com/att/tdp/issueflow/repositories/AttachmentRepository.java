package com.att.tdp.issueflow.repositories;

import com.att.tdp.issueflow.entities.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    boolean existsByIdAndTicketId(Long id, Long ticketId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Attachment a where a.id = :id and a.ticketId = :ticketId")
    int deleteByIdAndTicketId(@Param("id") Long id, @Param("ticketId") Long ticketId);
}