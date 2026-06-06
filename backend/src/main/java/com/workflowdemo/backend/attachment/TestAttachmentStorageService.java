package com.workflowdemo.backend.attachment;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("test")
class TestAttachmentStorageService implements AttachmentStorageService {

    @Override
    public String store(
        UUID applicationId,
        UUID attachmentId,
        String originalFilename,
        String contentType,
        long sizeBytes,
        InputStream inputStream
    ) {
        try {
            inputStream.transferTo(OutputStream.nullOutputStream());
        } catch (Exception exception) {
            throw new IllegalStateException("Attachment upload failed", exception);
        }
        return "test/applications/%s/%s-%s".formatted(applicationId, attachmentId, originalFilename);
    }
}
