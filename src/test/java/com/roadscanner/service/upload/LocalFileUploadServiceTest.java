package com.roadscanner.service.upload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.roadscanner.dao.qna.QuestionDAO;
import com.roadscanner.dao.upload.FileUploadDao;
import com.roadscanner.domain.upload.FileUploadVO;

public class LocalFileUploadServiceTest {

    private static final String PUBLIC_BASE_URL = "http://127.0.0.1:18080";

    private FileUploadDao fileUploadDao;
    private QuestionDAO questionDAO;
    private LocalFileUploadService service;

    @Before
    public void setUp() {
        fileUploadDao = mock(FileUploadDao.class);
        questionDAO = mock(QuestionDAO.class);
        service = new LocalFileUploadService(fileUploadDao, questionDAO, PUBLIC_BASE_URL);
    }

    @Test
    public void saveKeepsCanonicalBytesInsideTheProcess() throws Exception {
        when(fileUploadDao.doSave(any(FileUploadVO.class))).thenReturn(1);
        FileUploadVO metadata = new FileUploadVO();
        metadata.setId("localuser");
        metadata.setCategory(10);

        String key = service.doSave(validPng(), metadata);

        assertTrue(key.matches("[0-9a-f-]{36}\\.png"));
        assertEquals(PUBLIC_BASE_URL + "/local-files/" + key, metadata.getUrl());
        byte[] firstRead = service.read(key);
        assertNotNull(firstRead);
        firstRead[0] = 0;
        assertTrue(service.read(key)[0] != 0);
    }

    @Test
    public void failedDatabaseSaveDoesNotExposeAFile() throws Exception {
        when(fileUploadDao.doSave(any(FileUploadVO.class))).thenReturn(0);
        FileUploadVO metadata = new FileUploadVO();

        assertEquals("0", service.doSave(validPng(), metadata));
        assertNull(service.read(metadata.getName()));
    }

    @Test
    public void referencedQuestionAttachmentCannotBeDeleted() throws Exception {
        FileUploadVO attachment = storedFile();
        attachment.setIdx(7);
        attachment.setCategory(40);
        when(questionDAO.countByAttachmentId(7L)).thenReturn(1);

        try {
            service.doDelete(attachment);
            fail("Expected referenced attachment rejection");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("Referenced"));
        }

        verify(fileUploadDao, never()).doDelete(any(FileUploadVO.class));
    }

    @Test
    public void deleteRemovesDatabaseRow() throws Exception {
        FileUploadVO file = storedFile();
        when(fileUploadDao.doDelete(file)).thenReturn(1);

        assertEquals(1, service.doDelete(file));
        verify(fileUploadDao).doDelete(file);
    }

    @Test
    public void successfulDeleteRemovesPreviouslySavedBytes() throws Exception {
        when(fileUploadDao.doSave(any(FileUploadVO.class))).thenReturn(1);
        when(fileUploadDao.doDelete(any(FileUploadVO.class))).thenReturn(1);
        FileUploadVO metadata = new FileUploadVO();
        metadata.setCategory(10);

        String key = service.doSave(validPng(), metadata);
        assertNotNull(service.read(key));

        assertEquals(1, service.doDelete(metadata));
        assertNull(service.read(key));
    }

    @Test
    public void failedDatabaseDeleteKeepsPreviouslySavedBytes() throws Exception {
        when(fileUploadDao.doSave(any(FileUploadVO.class))).thenReturn(1);
        when(fileUploadDao.doDelete(any(FileUploadVO.class))).thenReturn(0);
        FileUploadVO metadata = new FileUploadVO();
        metadata.setCategory(10);

        String key = service.doSave(validPng(), metadata);

        assertEquals(0, service.doDelete(metadata));
        assertNotNull(service.read(key));
    }

    @Test
    public void readRejectsPathTraversalAndNonCanonicalObjectKeys() {
        assertNull(service.read("../private.png"));
        assertNull(service.read("00000000-0000-0000-0000-000000000001.png/../../private"));
        assertNull(service.read("ordinary-name.png"));
        assertNull(service.read(null));
    }

    @Test
    public void checkedUpdateAcceptsOnlyCanonicalStoredMetadata() throws Exception {
        FileUploadVO stored = storedFile();
        when(fileUploadDao.doUpdate(stored)).thenReturn(1);

        assertEquals(1, service.checkedUpdate(stored));
        assertEquals(1, stored.getChecked());
        verify(fileUploadDao).doUpdate(stored);

        FileUploadVO forged = new FileUploadVO();
        forged.setName("../forged.png");
        try {
            service.checkedUpdate(forged);
            fail("Expected invalid object key rejection");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("invalid"));
        }
        verify(fileUploadDao, never()).doUpdate(forged);
    }

    @Test(expected = IllegalArgumentException.class)
    public void saveRejectsMissingMetadataBeforePersisting() throws Exception {
        service.doSave(validPng(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsNonLoopbackPublicBaseUrl() {
        new LocalFileUploadService(fileUploadDao, questionDAO, "https://public.example.test");
    }

    private MockMultipartFile validPng() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return new MockMultipartFile(
                "fileUpload", "local.png", "image/png", output.toByteArray());
    }

    private FileUploadVO storedFile() {
        FileUploadVO file = new FileUploadVO();
        file.setName("00000000-0000-0000-0000-000000000001.png");
        file.setCategory(10);
        return file;
    }
}
