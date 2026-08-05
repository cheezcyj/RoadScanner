package com.roadscanner.service.upload;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.roadscanner.dao.qna.QuestionDAO;
import com.roadscanner.dao.upload.FileUploadDao;
import com.roadscanner.domain.upload.FileUploadVO;

/**
 * Isolated storage adapter used only by the local smoke-test profile.
 * Canonical image bytes stay in process memory and are never sent externally.
 */
@Service
@Profile("local")
public class LocalFileUploadService implements FileUploadService {

    private static final int QUESTION_ATTACHMENT_CATEGORY = 40;

    private final FileUploadDao fileUploadDao;
    private final QuestionDAO questionDAO;
    private final String publicBaseUrl;
    private final ConcurrentMap<String, byte[]> files = new ConcurrentHashMap<>();

    public LocalFileUploadService(
            @Qualifier("fileUploadDaoImpl") FileUploadDao fileUploadDao,
            QuestionDAO questionDAO,
            @Value("${roadscanner.local.public-base-url}") String publicBaseUrl) {
        this.fileUploadDao = fileUploadDao;
        this.questionDAO = questionDAO;
        this.publicBaseUrl = normalizeLoopbackBaseUrl(publicBaseUrl);
    }

    @Override
    public List<FileUploadVO> monthlyFeedback(FileUploadVO inVO) throws SQLException {
        return fileUploadDao.monthlyFeedback(inVO);
    }

    @Override
    public FileUploadVO totalFeedback(FileUploadVO inVO) throws SQLException {
        return fileUploadDao.totalFeedback(inVO);
    }

    @Override
    public List<FileUploadVO> doRetrieveByCategory(FileUploadVO inVO) throws SQLException {
        return fileUploadDao.doRetrieveByCategory(inVO);
    }

    @Override
    public List<FileUploadVO> doRetrieve(FileUploadVO inVO) throws SQLException {
        return fileUploadDao.doRetrieve(inVO);
    }

    @Override
    public FileUploadVO doSelectOne(FileUploadVO inVO) throws SQLException {
        return fileUploadDao.doSelectOne(inVO);
    }

    @Override
    public int doUpdate(FileUploadVO inVO) throws SQLException {
        return fileUploadDao.doUpdate(inVO);
    }

    @Override
    public int checkedUpdate(FileUploadVO inVO) throws SQLException {
        requireStoredFile(inVO);
        inVO.setChecked(1);
        return fileUploadDao.doUpdate(inVO);
    }

    @Override
    public int doDelete(FileUploadVO inVO) throws SQLException {
        requireStoredFile(inVO);
        if (inVO.getCategory() == QUESTION_ATTACHMENT_CATEGORY
                && inVO.getIdx() > 0
                && questionDAO.countByAttachmentId((long) inVO.getIdx()) > 0) {
            throw new IllegalStateException("Referenced question attachments cannot be deleted");
        }

        int deleted = fileUploadDao.doDelete(inVO);
        if (deleted == 1) {
            files.remove(inVO.getName());
        }
        return deleted;
    }

    @Override
    public int retryPendingDeletes(int batchSize) {
        // The local adapter deletes synchronously and has no external storage to retry.
        return 0;
    }

    @Override
    public String doSave(MultipartFile file, FileUploadVO inVO) throws SQLException, IOException {
        if (inVO == null) {
            throw new IllegalArgumentException("File metadata is required");
        }

        FileUploadServiceImpl.ValidatedImage image =
                FileUploadServiceImpl.validateAndCanonicalizeUpload(file);
        String originalFilename = image.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.'))
                .toLowerCase(Locale.ROOT);
        String objectKey = UUID.randomUUID().toString() + extension;
        byte[] content = image.getContent();

        inVO.setChecked(0);
        inVO.setName(objectKey);
        inVO.setUrl(publicBaseUrl + "/local-files/" + objectKey);
        inVO.setUploadDate(new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()));
        inVO.setFileSize(content.length / 1024.0);

        if (fileUploadDao.doSave(inVO) != 1) {
            return "0";
        }
        files.put(objectKey, content);
        return objectKey;
    }

    public byte[] read(String objectKey) {
        if (!isSafeObjectKey(objectKey)) {
            return null;
        }
        byte[] content = files.get(objectKey);
        return content == null ? null : Arrays.copyOf(content, content.length);
    }

    private void requireStoredFile(FileUploadVO file) {
        if (file == null || !isSafeObjectKey(file.getName())) {
            throw new IllegalArgumentException("Stored file metadata is invalid");
        }
    }

    private boolean isSafeObjectKey(String objectKey) {
        return objectKey != null && objectKey.matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(?:jpg|jpeg|png|bmp)");
    }

    private String normalizeLoopbackBaseUrl(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Local public base URL is required");
        }
        try {
            URI uri = new URI(value.trim());
            String host = uri.getHost();
            boolean loopback = "localhost".equalsIgnoreCase(host)
                    || "127.0.0.1".equals(host)
                    || "::1".equals(host);
            if (!("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))
                    || !loopback
                    || uri.getUserInfo() != null
                    || uri.getQuery() != null
                    || uri.getFragment() != null
                    || (uri.getPath() != null && !uri.getPath().isEmpty()
                        && !"/".equals(uri.getPath()))) {
                throw new IllegalArgumentException("Local public base URL must be a loopback origin");
            }
            String normalized = value.trim();
            while (normalized.endsWith("/")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            return normalized;
        } catch (URISyntaxException invalidUrl) {
            throw new IllegalArgumentException("Local public base URL is invalid", invalidUrl);
        }
    }
}
