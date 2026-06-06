package com.workflowdemo.backend.attachment;

import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
class MinioAttachmentStorageService implements AttachmentStorageService {

    private final MinioClient minioClient;
    private final String bucket;

    MinioAttachmentStorageService(
        @Value("${workflow.storage.minio.endpoint}") String endpoint,
        @Value("${workflow.storage.minio.access-key}") String accessKey,
        @Value("${workflow.storage.minio.secret-key}") String secretKey,
        @Value("${workflow.storage.minio.bucket}") String bucket
    ) {
        this.minioClient = MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build();
        this.bucket = bucket;
    }

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
            ensureBucket();
            String objectKey = "applications/%s/%s-%s".formatted(
                applicationId,
                attachmentId,
                sanitizeFilename(originalFilename)
            );
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(inputStream, sizeBytes, -1L)
                    .contentType(contentType)
                    .build()
            );
            return objectKey;
        } catch (Exception exception) {
            throw new IllegalStateException("Attachment upload failed", exception);
        }
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(
            BucketExistsArgs.builder()
                .bucket(bucket)
                .build()
        );
        if (!exists) {
            minioClient.makeBucket(
                MakeBucketArgs.builder()
                    .bucket(bucket)
                    .build()
            );
        }
    }

    private static String sanitizeFilename(String filename) {
        return filename.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
    }
}
