package com.roadscanner.service.upload;

import com.roadscanner.cmn.AppLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

/** Periodically drains a bounded batch of durable checked=-1/-2 deletions. */
@Component
public class PendingFileDeleteScheduler implements AppLogger {

    private final FileUploadService fileUploadService;

    @Value("${roadscanner.storage.delete-retry-batch-size:25}")
    private int batchSize;

    public PendingFileDeleteScheduler(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @Scheduled(fixedDelayString = "${roadscanner.storage.delete-retry-delay-ms:60000}")
    public void retryPendingDeletes() {
        try {
            fileUploadService.retryPendingDeletes(batchSize);
        } catch (SQLException | RuntimeException retryFailure) {
            // Do not include storage keys, URLs, credentials, or exception payloads.
            LOG.warn("Pending image deletion batch could not be loaded");
        }
    }
}
