package com.tms.attachment.repository;

import com.tms.attachment.entity.Attachment;
import com.tms.attachment.entity.AttachmentEntityType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findAllByEntityTypeAndEntityIdOrderByCreatedAtAsc(
            AttachmentEntityType entityType, Long entityId);

    void deleteAllByEntityTypeAndEntityId(AttachmentEntityType entityType, Long entityId);
}
