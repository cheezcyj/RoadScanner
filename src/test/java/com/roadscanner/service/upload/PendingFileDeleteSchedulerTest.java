package com.roadscanner.service.upload;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.SQLException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PendingFileDeleteSchedulerTest {

    @Test
    public void scheduledRunDelegatesConfiguredBatchSize() throws Exception {
        FileUploadService service = mock(FileUploadService.class);
        PendingFileDeleteScheduler scheduler = new PendingFileDeleteScheduler(service);
        ReflectionTestUtils.setField(scheduler, "batchSize", 12);

        scheduler.retryPendingDeletes();

        verify(service).retryPendingDeletes(12);
    }

    @Test
    public void scheduledRunContainsBatchLoadFailure() throws Exception {
        FileUploadService service = mock(FileUploadService.class);
        when(service.retryPendingDeletes(12)).thenThrow(new SQLException("database unavailable"));
        PendingFileDeleteScheduler scheduler = new PendingFileDeleteScheduler(service);
        ReflectionTestUtils.setField(scheduler, "batchSize", 12);

        scheduler.retryPendingDeletes();

        verify(service).retryPendingDeletes(12);
    }
}
