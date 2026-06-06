package com.workflowdemo.backend.attachment;

import java.io.InputStream;
import java.util.UUID;

interface AttachmentStorageService {

    String store(
        UUID applicationId,
        UUID attachmentId,
        String originalFilename,
        String contentType,
        long sizeBytes,
        InputStream inputStream
    );
}
