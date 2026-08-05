package com.roadscanner.service.upload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;

import javax.imageio.ImageIO;

import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.roadscanner.dao.upload.FileUploadDao;
import com.roadscanner.dao.qna.QuestionDAO;
import com.roadscanner.domain.upload.FileUploadVO;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

public class FileUploadServiceImplTest {

    @Test
    public void validateUploadFileAcceptsSafeImage() throws Exception {
        MockMultipartFile file = image("road sign.jpg", "image/jpeg", imageBytes("jpg"));

        assertEquals("road sign.jpg", FileUploadServiceImpl.validateUploadFile(file));
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateUploadFileRejectsNull() {
        FileUploadServiceImpl.validateUploadFile(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateUploadFileRejectsEmptyFile() {
        FileUploadServiceImpl.validateUploadFile(image("empty.png", "image/png", new byte[0]));
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateUploadFileRejectsOversizedFile() {
        FileUploadServiceImpl.validateUploadFile(
                image("large.png", "image/png", new byte[5 * 1024 * 1024 + 1]));
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateUploadFileRejectsParentPath() {
        FileUploadServiceImpl.validateUploadFile(image("../road.jpg", "image/jpeg", new byte[] { 1 }));
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateUploadFileRejectsWindowsPath() {
        String windowsPath = String.join("\\", "C:", "temp", "road.jpg");
        FileUploadServiceImpl.validateUploadFile(
                image(windowsPath, "image/jpeg", new byte[] { 1 }));
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateUploadFileRejectsUnsupportedExtension() throws Exception {
        FileUploadServiceImpl.validateUploadFile(image("road.exe", "image/jpeg", imageBytes("jpg")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateUploadFileRejectsSpoofedImagePrefix() throws Exception {
        FileUploadServiceImpl.validateUploadFile(
                image("road.jpg", "image-malicious", imageBytes("jpg")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateUploadFileRejectsSignatureThatDoesNotMatchExtension() throws Exception {
        FileUploadServiceImpl.validateUploadFile(image("road.jpg", "image/jpeg", imageBytes("png")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateUploadFileRejectsCorruptContentWithValidPrefix() {
        FileUploadServiceImpl.validateUploadFile(image(
                "road.png", "image/png",
                new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3 }));
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateUploadFileRejectsImageWithExcessivePixelCount() {
        FileUploadServiceImpl.validateUploadFile(
                image("huge.bmp", "image/bmp", bmpHeader(3000, 3000)));
    }

    @Test
    public void canonicalImageRemovesTrailingPayload() throws Exception {
        byte[] image = imageBytes("png");
        byte[] marker = "PRIVATE_TRAILING_PAYLOAD".getBytes("UTF-8");
        byte[] withPayload = new byte[image.length + marker.length];
        System.arraycopy(image, 0, withPayload, 0, image.length);
        System.arraycopy(marker, 0, withPayload, image.length, marker.length);

        FileUploadServiceImpl.ValidatedImage validated =
                FileUploadServiceImpl.validateAndCanonicalizeUpload(
                        image("road.png", "image/png", withPayload));

        assertFalse(contains(validated.getContent(), marker));
        assertTrue(validated.getContent().length < withPayload.length);
    }

    @Test
    public void canonicalJpegAppliesExifOrientationAndRemovesMetadata() throws Exception {
        BufferedImage source = new BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(source, "jpg", jpeg));
        byte[] exifMarker = "Exif\0\0".getBytes("ISO-8859-1");
        byte[] orientedJpeg = withExifOrientation(jpeg.toByteArray(), 6);

        FileUploadServiceImpl.ValidatedImage validated =
                FileUploadServiceImpl.validateAndCanonicalizeUpload(
                        image("phone.jpg", "image/jpeg", orientedJpeg));
        BufferedImage canonical = ImageIO.read(new ByteArrayInputStream(validated.getContent()));

        assertEquals(3, canonical.getWidth());
        assertEquals(2, canonical.getHeight());
        assertFalse(contains(validated.getContent(), exifMarker));
    }

    @Test
    public void saveRemovesUploadedObjectWhenDatabaseReturnsZero() throws Exception {
        StorageStubService service = serviceWithDaoResult(0);

        assertEquals("0", service.doSave(validPng(), new FileUploadVO()));
        assertTrue(service.uploaded);
        assertTrue(service.deleted);
    }

    @Test
    public void saveKeepsUploadedObjectAfterDatabaseSuccess() throws Exception {
        StorageStubService service = serviceWithDaoResult(1);

        assertEquals("stored.png", service.doSave(validPng(), new FileUploadVO()));
        assertTrue(service.uploaded);
        assertFalse(service.deleted);
    }

    @Test
    public void saveUploadsOnlyCanonicalPayload() throws Exception {
        StorageStubService service = serviceWithDaoResult(1);
        byte[] image = imageBytes("png");
        byte[] marker = "PRIVATE_TRAILING_PAYLOAD".getBytes("UTF-8");
        byte[] withPayload = new byte[image.length + marker.length];
        System.arraycopy(image, 0, withPayload, 0, image.length);
        System.arraycopy(marker, 0, withPayload, image.length, marker.length);

        service.doSave(image("road.png", "image/png", withPayload), new FileUploadVO());

        assertFalse(contains(service.uploadedContent, marker));
    }

    @Test
    public void saveRemovesUploadedObjectAndRethrowsDatabaseFailure() throws Exception {
        StorageStubService service = new StorageStubService();
        service.dao = mock(FileUploadDao.class);
        SQLException expected = new SQLException("database unavailable");
        when(service.dao.doSave(any(FileUploadVO.class))).thenThrow(expected);

        try {
            service.doSave(validPng(), new FileUploadVO());
            fail("Expected SQLException");
        } catch (SQLException actual) {
            assertSame(expected, actual);
        }
        assertTrue(service.deleted);
    }

    @Test
    public void saveAttachesCleanupFailureToDatabaseFailure() throws Exception {
        StorageStubService service = new StorageStubService();
        service.dao = mock(FileUploadDao.class);
        service.failDelete = true;
        SQLException expected = new SQLException("database unavailable");
        when(service.dao.doSave(any(FileUploadVO.class))).thenThrow(expected);

        try {
            service.doSave(validPng(), new FileUploadVO());
            fail("Expected SQLException");
        } catch (SQLException actual) {
            assertSame(expected, actual);
            assertEquals(1, actual.getSuppressed().length);
            assertTrue(actual.getSuppressed()[0] instanceof IOException);
        }
    }

    @Test
    public void reviewedBucketCopyIsRemovedWhenDatabaseUpdateFails() throws Exception {
        S3Client s3 = mock(S3Client.class);
        when(s3.copyObject(any(CopyObjectRequest.class)))
                .thenReturn(CopyObjectResponse.builder().build());

        StorageStubService service = new StorageStubService(s3);
        service.dao = mock(FileUploadDao.class);
        ReflectionTestUtils.setField(service, "bucket", "incoming");
        ReflectionTestUtils.setField(service, "bucket2", "reviewed");
        when(service.dao.doUpdate(any(FileUploadVO.class))).thenThrow(new SQLException("write failed"));

        FileUploadVO file = storedFile();
        try {
            service.checkedUpdate(file);
            fail("Expected SQLException");
        } catch (SQLException expected) {
            // expected
        }

        verify(s3).copyObject(argThat((CopyObjectRequest request) ->
                "incoming".equals(request.sourceBucket())
                        && "stored.png".equals(request.sourceKey())
                        && "reviewed".equals(request.destinationBucket())
                        && "stored.png".equals(request.destinationKey())));
        verify(s3).deleteObject(argThat((DeleteObjectRequest request) ->
                "reviewed".equals(request.bucket()) && "stored.png".equals(request.key())));
        verify(s3, never()).deleteObject(argThat((DeleteObjectRequest request) ->
                "incoming".equals(request.bucket()) && "stored.png".equals(request.key())));
        assertEquals(0, file.getChecked());
        assertEquals("https://example.test/incoming/stored.png", file.getUrl());
    }

    @Test
    public void reviewedBucketCopyIsRemovedWhenUrlCreationFails() throws Exception {
        S3Client s3 = mock(S3Client.class);
        when(s3.copyObject(any(CopyObjectRequest.class)))
                .thenReturn(CopyObjectResponse.builder().build());

        StorageStubService service = new StorageStubService(s3);
        service.failObjectUrl = true;
        service.dao = mock(FileUploadDao.class);
        ReflectionTestUtils.setField(service, "bucket", "incoming");
        ReflectionTestUtils.setField(service, "bucket2", "reviewed");

        FileUploadVO file = storedFile();
        try {
            service.checkedUpdate(file);
            fail("Expected storage failure");
        } catch (IllegalStateException expected) {
            // expected
        }

        verify(s3).deleteObject(argThat((DeleteObjectRequest request) ->
                "reviewed".equals(request.bucket()) && "stored.png".equals(request.key())));
        verify(service.dao, never()).doUpdate(any(FileUploadVO.class));
        assertEquals(0, file.getChecked());
        assertEquals("https://example.test/incoming/stored.png", file.getUrl());
    }

    @Test
    public void uploadIsRemovedWhenUrlCreationFailsAfterPut() throws Exception {
        S3Client s3 = mock(S3Client.class);
        UploadFailureService service = new UploadFailureService(s3);
        ReflectionTestUtils.setField(service, "bucket", "incoming");

        try {
            service.doSave(validPng(), new FileUploadVO());
            fail("Expected storage failure");
        } catch (IllegalStateException expected) {
            // expected
        }

        verify(s3).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    public void uploadObjectKeyDoesNotExposeOriginalFilename() throws Exception {
        S3Client s3 = mock(S3Client.class);
        OpaqueKeyUploadService service = new OpaqueKeyUploadService(s3);
        ReflectionTestUtils.setField(service, "bucket", "incoming");

        FileUploadVO uploaded = service.uploadFileToS3(
                new byte[] { 1, 2, 3 }, "image/png", new FileUploadVO(),
                "personal-name-road.png");

        assertFalse(uploaded.getName().contains("personal-name-road"));
        assertTrue(uploaded.getName().matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.png"));
    }

    @Test
    public void deleteDoesNotRemoveObjectWhenPendingMarkerFails() throws Exception {
        StorageStubService service = new StorageStubService();
        service.dao = mock(FileUploadDao.class);
        when(service.dao.doUpdate(any(FileUploadVO.class))).thenThrow(new SQLException("write failed"));

        FileUploadVO file = storedFile();

        try {
            service.doDelete(file);
            fail("Expected SQLException");
        } catch (SQLException expected) {
            // expected
        }

        assertFalse(service.deleted);
        assertEquals(0, file.getChecked());
        verify(service.dao, never()).doDelete(any(FileUploadVO.class));
    }

    @Test
    public void deleteRejectsQuestionAttachmentThatIsStillReferenced() throws Exception {
        StorageStubService service = new StorageStubService();
        service.dao = mock(FileUploadDao.class);
        service.questionDAO = mock(QuestionDAO.class);
        FileUploadVO file = storedFile();
        file.setIdx(17);
        file.setCategory(40);
        when(service.questionDAO.countByAttachmentId(17L)).thenReturn(1);

        try {
            service.doDelete(file);
            fail("Expected referenced attachment rejection");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("Referenced"));
        }

        assertFalse(service.deleted);
        verify(service.dao, never()).doUpdate(any(FileUploadVO.class));
        verify(service.dao, never()).doDelete(any(FileUploadVO.class));
    }

    @Test
    public void deleteKeepsPendingMarkerWhenFinalDatabaseDeleteFails() throws Exception {
        StorageStubService service = new StorageStubService();
        service.dao = mock(FileUploadDao.class);
        when(service.dao.doUpdate(any(FileUploadVO.class))).thenReturn(1);
        when(service.dao.doDelete(any(FileUploadVO.class))).thenThrow(new SQLException("write failed"));

        FileUploadVO file = storedFile();
        try {
            service.doDelete(file);
            fail("Expected SQLException");
        } catch (SQLException expected) {
            // expected
        }

        assertTrue(service.deleted);
        assertEquals(-1, file.getChecked());
    }

    @Test
    public void deleteKeepsPendingMarkerWhenStorageDeleteFails() throws Exception {
        StorageStubService service = new StorageStubService();
        service.dao = mock(FileUploadDao.class);
        service.failDelete = true;
        when(service.dao.doUpdate(any(FileUploadVO.class))).thenReturn(1);

        FileUploadVO file = storedFile();
        try {
            service.doDelete(file);
            fail("Expected IOException");
        } catch (IOException expected) {
            // expected
        }

        assertEquals(-1, file.getChecked());
        verify(service.dao, never()).doDelete(any(FileUploadVO.class));
    }

    @Test
    public void retryOfPendingReviewedDeleteUsesReviewedBucket() throws Exception {
        S3Client s3 = mock(S3Client.class);
        S3DeleteService service = new S3DeleteService(s3);
        service.dao = mock(FileUploadDao.class);
        when(service.dao.doDelete(any(FileUploadVO.class))).thenReturn(1);
        ReflectionTestUtils.setField(service, "bucket", "incoming");
        ReflectionTestUtils.setField(service, "bucket2", "reviewed");

        FileUploadVO file = storedFile();
        file.setChecked(-2);
        assertEquals(1, service.doDelete(file));

        verify(s3).deleteObject(argThat((DeleteObjectRequest request) ->
                "reviewed".equals(request.bucket()) && "stored.png".equals(request.key())));
        verify(service.dao, never()).doUpdate(any(FileUploadVO.class));
    }

    @Test
    public void pendingRetryRestoresReviewedStateWhenQuestionStillReferencesFile() throws Exception {
        StorageStubService service = new StorageStubService();
        service.dao = mock(FileUploadDao.class);
        service.questionDAO = mock(QuestionDAO.class);
        FileUploadVO file = storedFile();
        file.setIdx(17);
        file.setChecked(-2);
        when(service.dao.findPendingDeletes(10)).thenReturn(Collections.singletonList(file));
        when(service.questionDAO.countByAttachmentId(17L)).thenReturn(1);
        when(service.dao.restorePendingDelete(17, file.getName(), -2, 1)).thenReturn(1);

        assertEquals(0, service.retryPendingDeletes(10));

        assertEquals(1, file.getChecked());
        assertFalse(service.deleted);
        verify(service.dao).restorePendingDelete(17, file.getName(), -2, 1);
        verify(service.dao, never()).doDelete(any(FileUploadVO.class));
    }

    @Test
    public void pendingRetryDeletesStorageIdempotentlyThenDatabaseRow() throws Exception {
        StorageStubService service = new StorageStubService();
        service.dao = mock(FileUploadDao.class);
        service.questionDAO = mock(QuestionDAO.class);
        FileUploadVO file = storedFile();
        file.setIdx(17);
        file.setChecked(-1);
        when(service.dao.findPendingDeletes(10)).thenReturn(Collections.singletonList(file));
        when(service.dao.doDelete(file)).thenReturn(1);

        assertEquals(1, service.retryPendingDeletes(10));

        assertTrue(service.deleted);
        verify(service.questionDAO).countByAttachmentId(17L);
        verify(service.dao).doDelete(file);
    }

    @Test
    public void pendingRetryKeepsMarkerWhenStorageStillFails() throws Exception {
        StorageStubService service = new StorageStubService();
        service.dao = mock(FileUploadDao.class);
        service.questionDAO = mock(QuestionDAO.class);
        service.failDelete = true;
        FileUploadVO file = storedFile();
        file.setIdx(17);
        file.setChecked(-1);
        when(service.dao.findPendingDeletes(10)).thenReturn(Collections.singletonList(file));

        assertEquals(0, service.retryPendingDeletes(10));

        assertEquals(-1, file.getChecked());
        verify(service.dao, never()).doDelete(any(FileUploadVO.class));
    }

    @Test
    public void pendingRetryCapsConfiguredBatchSize() throws Exception {
        StorageStubService service = new StorageStubService();
        service.dao = mock(FileUploadDao.class);
        when(service.dao.findPendingDeletes(100)).thenReturn(Collections.emptyList());

        assertEquals(0, service.retryPendingDeletes(1000));

        verify(service.dao).findPendingDeletes(100);
    }

    private StorageStubService serviceWithDaoResult(int result) throws SQLException {
        StorageStubService service = new StorageStubService();
        service.dao = mock(FileUploadDao.class);
        when(service.dao.doSave(any(FileUploadVO.class))).thenReturn(result);
        return service;
    }

    private MockMultipartFile validPng() throws IOException {
        return image("road.png", "image/png", imageBytes("png"));
    }

    private MockMultipartFile image(String originalFilename, String contentType, byte[] content) {
        return new MockMultipartFile("fileUpload", originalFilename, contentType, content);
    }

    private byte[] imageBytes(String format) throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, format, output)) {
            throw new IOException("No image writer for " + format);
        }
        return output.toByteArray();
    }

    private byte[] bmpHeader(int width, int height) {
        byte[] header = new byte[54];
        header[0] = 'B';
        header[1] = 'M';
        writeLittleEndianInt(header, 2, header.length);
        writeLittleEndianInt(header, 10, 54);
        writeLittleEndianInt(header, 14, 40);
        writeLittleEndianInt(header, 18, width);
        writeLittleEndianInt(header, 22, height);
        header[26] = 1;
        header[28] = 24;
        return header;
    }

    private void writeLittleEndianInt(byte[] target, int offset, int value) {
        target[offset] = (byte) value;
        target[offset + 1] = (byte) (value >>> 8);
        target[offset + 2] = (byte) (value >>> 16);
        target[offset + 3] = (byte) (value >>> 24);
    }

    private boolean contains(byte[] content, byte[] marker) {
        outer:
        for (int offset = 0; offset <= content.length - marker.length; offset++) {
            for (int index = 0; index < marker.length; index++) {
                if (content[offset + index] != marker[index]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    private byte[] withExifOrientation(byte[] jpeg, int orientation) throws IOException {
        byte[] exif = new byte[] {
                'E', 'x', 'i', 'f', 0, 0,
                'M', 'M', 0, 42, 0, 0, 0, 8,
                0, 1,
                1, 18, 0, 3, 0, 0, 0, 1,
                0, (byte) orientation, 0, 0,
                0, 0, 0, 0
        };
        int segmentLength = exif.length + 2;
        ByteArrayOutputStream output = new ByteArrayOutputStream(jpeg.length + exif.length + 4);
        output.write(jpeg, 0, 2);
        output.write(0xFF);
        output.write(0xE1);
        output.write(segmentLength >>> 8);
        output.write(segmentLength);
        output.write(exif);
        output.write(jpeg, 2, jpeg.length - 2);
        return output.toByteArray();
    }

    private FileUploadVO storedFile() {
        FileUploadVO file = new FileUploadVO();
        file.setName("stored.png");
        file.setChecked(0);
        file.setUrl("https://example.test/incoming/stored.png");
        return file;
    }

    private static class StorageStubService extends FileUploadServiceImpl {
        private final S3Client s3Client;
        private boolean uploaded;
        private boolean deleted;
        private boolean failDelete;
        private boolean failObjectUrl;
        private byte[] uploadedContent;

        private StorageStubService() {
            this(null);
        }

        private StorageStubService(S3Client s3Client) {
            this.s3Client = s3Client;
        }

        @Override
        protected FileUploadVO uploadFileToS3(byte[] content, String contentType,
                FileUploadVO uploadVO, String originalFilename) {
            uploaded = true;
            uploadedContent = content.clone();
            uploadVO.setName("stored.png");
            uploadVO.setUrl("https://example.test/stored.png");
            return uploadVO;
        }

        @Override
        protected void deleteFileToS3(FileUploadVO inVO) throws IOException {
            deleted = true;
            if (failDelete) {
                throw new IOException("cleanup failed");
            }
        }

        @Override
        protected S3Client createS3Client() {
            return s3Client == null ? super.createS3Client() : s3Client;
        }

        @Override
        protected String buildObjectUrl(S3Client client, String targetBucket, String objectKey) {
            if (failObjectUrl) {
                throw new IllegalStateException("url unavailable");
            }
            return "https://example.test/" + objectKey;
        }
    }

    private static class UploadFailureService extends FileUploadServiceImpl {
        private final S3Client s3Client;

        private UploadFailureService(S3Client s3Client) {
            this.s3Client = s3Client;
        }

        @Override
        protected S3Client createS3Client() {
            return s3Client;
        }

        @Override
        protected String buildObjectUrl(S3Client client, String targetBucket, String objectKey) {
            throw new IllegalStateException("url unavailable");
        }
    }

    private static class OpaqueKeyUploadService extends FileUploadServiceImpl {
        private final S3Client s3Client;

        private OpaqueKeyUploadService(S3Client s3Client) {
            this.s3Client = s3Client;
        }

        @Override
        protected S3Client createS3Client() {
            return s3Client;
        }

        @Override
        protected String buildObjectUrl(S3Client client, String targetBucket, String objectKey) {
            return "https://example.test/" + objectKey;
        }
    }

    private static class S3DeleteService extends FileUploadServiceImpl {
        private final S3Client s3Client;

        private S3DeleteService(S3Client s3Client) {
            this.s3Client = s3Client;
        }

        @Override
        protected S3Client createS3Client() {
            return s3Client;
        }
    }
}
