package com.att.tdp.issueflow.services;

import org.springframework.transaction.annotation.Transactional;
import com.att.tdp.issueflow.entities.Attachment;
import com.att.tdp.issueflow.repositories.AttachmentRepository;
import com.att.tdp.issueflow.repositories.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "application/pdf",
            "text/plain"
    );

    private final AttachmentRepository attachmentRepository;
    private final TicketRepository ticketRepository;
    private final AuditLogService auditLogService;

    public Attachment uploadAttachment(Long ticketId, MultipartFile file) {
        validateTicketExists(ticketId);
        validateFile(file);

        try {
            String originalFilename = file.getOriginalFilename();
            String filename = originalFilename == null || originalFilename.isBlank()
                    ? "attachment"
                    : originalFilename;

            Attachment attachment = Attachment.builder()
                    .ticketId(ticketId)
                    .filename(filename)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .data(file.getBytes())
                    .build();

            Attachment savedAttachment = attachmentRepository.save(attachment);
            auditLogService.logUserAction("UPLOAD_ATTACHMENT", "TICKET", ticketId);

            return savedAttachment;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file");
        }
    }

    @Transactional
    public void deleteAttachment(Long ticketId, Long attachmentId) {
        validateTicketExists(ticketId);

        if (!attachmentRepository.existsByIdAndTicketId(attachmentId, ticketId)) {
            throw new IllegalArgumentException("Attachment not found");
        }

        int deletedRows = attachmentRepository.deleteByIdAndTicketId(attachmentId, ticketId);

        if (deletedRows == 0) {
            throw new IllegalArgumentException("Attachment not found");
        }

        auditLogService.logUserAction("DELETE_ATTACHMENT", "TICKET", ticketId);
    }

    private void validateTicketExists(Long ticketId) {
        if (ticketRepository.findByIdAndDeletedFalse(ticketId).isEmpty()) {
            throw new IllegalArgumentException("Ticket does not exist");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds 10 MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported file type");
        }
    }
}