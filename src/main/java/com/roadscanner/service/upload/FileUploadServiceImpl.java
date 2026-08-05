package com.roadscanner.service.upload;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.roadscanner.cmn.AppLogger;
import com.roadscanner.cmn.validation.ImageFileValidator;
import com.roadscanner.dao.upload.FileUploadDao;
import com.roadscanner.dao.qna.QuestionDAO;
import com.roadscanner.domain.upload.FileUploadVO;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@Profile("!local")
public class FileUploadServiceImpl implements AppLogger, FileUploadService {

    private static final long MAX_UPLOAD_SIZE = 5L * 1024 * 1024;
    private static final long MAX_IMAGE_PIXELS = 8_000_000L;
    private static final int MAX_IMAGE_DIMENSION = 6_000;
    private static final int PENDING_DELETE_INCOMING = -1;
    private static final int PENDING_DELETE_REVIEWED = -2;
    private static final int QUESTION_ATTACHMENT_CATEGORY = 40;
    private static final int MAX_DELETE_RETRY_BATCH_SIZE = 100;

    @Autowired
    @Qualifier("fileUploadDaoImpl")
    FileUploadDao dao;

    @Autowired
    QuestionDAO questionDAO;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${cloud.aws.credentials.accessKey}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secretKey}")
    private String secretKey;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.bucket2}")
    private String bucket2;

    static final class ValidatedImage {
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        private ValidatedImage(String originalFilename, String contentType, byte[] content) {
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        String getOriginalFilename() {
            return originalFilename;
        }

        String getContentType() {
            return contentType;
        }

        byte[] getContent() {
            return content.clone();
        }
    }

    @Override
    public int checkedUpdate(FileUploadVO inVO) throws SQLException, IOException {
        validateStoredFile(inVO);

        String sourceKey = inVO.getName();
        String destinationKey = sourceKey;
        int previousChecked = inVO.getChecked();
        String previousUrl = inVO.getUrl();
        S3Client s3Client = createS3Client();
        boolean copied = false;
        boolean databaseUpdated = false;
        try {
            CopyObjectResponse copyResult = s3Client.copyObject(CopyObjectRequest.builder()
                    .sourceBucket(bucket)
                    .sourceKey(sourceKey)
                    .destinationBucket(bucket2)
                    .destinationKey(destinationKey)
                    .build());
            if (copyResult == null) {
                return 0;
            }
            copied = true;

            inVO.setChecked(1);
            inVO.setUrl(buildObjectUrl(s3Client, bucket2, destinationKey));

            int updated = dao.doUpdate(inVO);
            if (updated != 1) {
                try {
                    deleteObject(s3Client, bucket2, destinationKey);
                    copied = false;
                } finally {
                    restoreStoredFileState(inVO, previousChecked, previousUrl);
                }
                return updated;
            }
            databaseUpdated = true;

            // Delete the source only after the database points at the copied object.
            deleteObject(s3Client, bucket, sourceKey);
            LOG.debug("Stored image moved to the reviewed bucket");
            return updated;
        } catch (SQLException | RuntimeException failure) {
            if (copied && !databaseUpdated) {
                compensateCopiedObject(s3Client, destinationKey, failure);
            }
            if (!databaseUpdated) {
                restoreStoredFileState(inVO, previousChecked, previousUrl);
            }
            throw failure;
        } finally {
            try {
                s3Client.close();
            } catch (RuntimeException closeFailure) {
                LOG.warn("Failed to close the image storage client");
            }
        }
    }

    @Override
    public List<FileUploadVO> monthlyFeedback(FileUploadVO inVO) throws SQLException {
        return dao.monthlyFeedback(inVO);
    }

    @Override
    public FileUploadVO totalFeedback(FileUploadVO inVO) throws SQLException {
        return dao.totalFeedback(inVO);
    }

    @Override
    public List<FileUploadVO> doRetrieveByCategory(FileUploadVO inVO) throws SQLException {
        return dao.doRetrieveByCategory(inVO);
    }

    @Override
    public List<FileUploadVO> doRetrieve(FileUploadVO inVO) throws SQLException {
        return dao.doRetrieve(inVO);
    }

    @Override
    public FileUploadVO doSelectOne(FileUploadVO inVO) throws SQLException {
        return dao.doSelectOne(inVO);
    }

    @Override
    public int doUpdate(FileUploadVO inVO) throws SQLException {
        return dao.doUpdate(inVO);
    }

    @Override
    public int doDelete(FileUploadVO inVO) throws SQLException, IOException {
        validateStoredFile(inVO);
        if (inVO.getCategory() == QUESTION_ATTACHMENT_CATEGORY
                && inVO.getIdx() > 0
                && questionDAO.countByAttachmentId((long) inVO.getIdx()) > 0) {
            throw new IllegalStateException("Referenced question attachments cannot be deleted");
        }

        int previousChecked = inVO.getChecked();
        boolean alreadyPending = isPendingDelete(previousChecked);
        if (!alreadyPending) {
            inVO.setChecked(usesReviewedBucket(previousChecked)
                    ? PENDING_DELETE_REVIEWED
                    : PENDING_DELETE_INCOMING);
            int marked;
            try {
                marked = dao.doUpdate(inVO);
            } catch (SQLException | RuntimeException failure) {
                inVO.setChecked(previousChecked);
                throw failure;
            }
            if (marked != 1) {
                inVO.setChecked(previousChecked);
                return 0;
            }
        }

        // The pending row preserves the bucket and key if storage cleanup must be retried.
        deleteFileToS3(inVO);
        return dao.doDelete(inVO);
    }

    @Override
    public int retryPendingDeletes(int batchSize) throws SQLException {
        int boundedBatchSize = Math.max(1,
                Math.min(batchSize, MAX_DELETE_RETRY_BATCH_SIZE));
        List<FileUploadVO> pendingDeletes = dao.findPendingDeletes(boundedBatchSize);
        int completed = 0;

        for (FileUploadVO pending : pendingDeletes) {
            try {
                if (pending == null || !isPendingDelete(pending.getChecked())) {
                    continue;
                }

                // Re-check immediately before the idempotent storage delete. If a
                // question still references the row, cancel the pending deletion.
                if (pending.getIdx() > 0
                        && questionDAO.countByAttachmentId((long) pending.getIdx()) > 0) {
                    restorePendingDelete(pending);
                    continue;
                }

                validateStoredFile(pending);
                deleteFileToS3(pending);
                if (dao.doDelete(pending) == 1) {
                    completed++;
                }
            } catch (SQLException | IOException | RuntimeException retryFailure) {
                // Keep the negative checked marker. A later bounded run retries the
                // idempotent object deletion and then the final database deletion.
                LOG.warn("Pending image deletion retry failed; it remains scheduled");
            }
        }
        return completed;
    }

    private void restorePendingDelete(FileUploadVO pending) throws SQLException {
        int pendingChecked = pending.getChecked();
        int restoredChecked = pendingChecked == PENDING_DELETE_REVIEWED ? 1 : 0;
        if (dao.restorePendingDelete(pending.getIdx(), pending.getName(),
                pendingChecked, restoredChecked) == 1) {
            pending.setChecked(restoredChecked);
        }
    }

    @Override
    public String doSave(MultipartFile file, FileUploadVO inVO) throws SQLException, IOException {
        if (inVO == null) {
            throw new IllegalArgumentException("File metadata is required");
        }

        ValidatedImage validatedImage = validateAndCanonicalizeUpload(file);
        // Storage state is server-owned and must not be accepted from request binding.
        inVO.setChecked(0);
        FileUploadVO uploadedFile = uploadFileToS3(
                validatedImage.content,
                validatedImage.contentType,
                inVO,
                validatedImage.originalFilename);

        try {
            if (dao.doSave(uploadedFile) == 1) {
                return uploadedFile.getName();
            }

            deleteFileToS3(uploadedFile);
            return "0";
        } catch (SQLException | RuntimeException failure) {
            compensateUploadedObject(uploadedFile, failure);
            throw failure;
        }
    }

    protected FileUploadVO uploadFileToS3(byte[] content, String contentType,
            FileUploadVO uploadVO, String originalFilename) throws IOException {
        String uploadDate = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.'))
                .toLowerCase(Locale.ROOT);
        // Keep user-provided names out of persistent URLs and use the full UUID space.
        String objectKey = UUID.randomUUID().toString() + extension;

        S3Client s3Client = createS3Client();
        boolean uploaded = false;
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(contentType)
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(content));
            uploaded = true;

            uploadVO.setUploadDate(uploadDate);
            uploadVO.setName(objectKey);
            uploadVO.setUrl(buildObjectUrl(s3Client, bucket, objectKey));
            uploadVO.setFileSize(content.length / 1024.0);
            LOG.debug("Image upload completed");
            return uploadVO;
        } catch (RuntimeException failure) {
            if (uploaded) {
                try {
                    s3Client.deleteObject(DeleteObjectRequest.builder()
                            .bucket(bucket).key(objectKey).build());
                } catch (RuntimeException cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw failure;
        } finally {
            try {
                s3Client.close();
            } catch (RuntimeException closeFailure) {
                LOG.warn("Failed to close the image storage client");
            }
        }
    }

    static String validateUploadFile(MultipartFile file) {
        return validateAndCanonicalizeUpload(file).originalFilename;
    }

    static ValidatedImage validateAndCanonicalizeUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("An image file is required");
        }
        if (file.getSize() > MAX_UPLOAD_SIZE) {
            throw new IllegalArgumentException("Image files must not exceed 5 MB");
        }
        if (!ImageFileValidator.hasSafeImageFilename(file.getOriginalFilename())) {
            throw new IllegalArgumentException("The image filename or extension is not allowed");
        }
        if (!ImageFileValidator.isSupportedContentType(file.getContentType())) {
            throw new IllegalArgumentException("The image content type is not supported");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("The image file could not be read", e);
        }
        if (content.length == 0 || content.length > MAX_UPLOAD_SIZE) {
            throw new IllegalArgumentException("The image size is invalid");
        }

        String contentType = normalizeContentType(file.getContentType());
        String extension = file.getOriginalFilename()
                .substring(file.getOriginalFilename().lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
        validateSignature(file.getOriginalFilename(), contentType, content);
        byte[] canonicalContent = canonicalizeImage(extension, content);
        if (canonicalContent.length == 0 || canonicalContent.length > MAX_UPLOAD_SIZE) {
            throw new IllegalArgumentException("The canonical image size is invalid");
        }
        validateSignature(file.getOriginalFilename(), contentType, canonicalContent);
        return new ValidatedImage(file.getOriginalFilename(), contentType, canonicalContent);
    }

    private static void validateSignature(String filename, String contentType, byte[] content) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        String normalizedType = normalizeContentType(contentType);

        boolean valid;
        switch (extension) {
            case "jpg":
            case "jpeg":
                valid = "image/jpeg".equals(normalizedType)
                        && startsWith(content, 0xFF, 0xD8, 0xFF);
                break;
            case "png":
                valid = "image/png".equals(normalizedType)
                        && startsWith(content, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
                break;
            case "bmp":
                valid = "image/bmp".equals(normalizedType) && startsWith(content, 0x42, 0x4D);
                break;
            default:
                valid = false;
        }

        if (!valid) {
            throw new IllegalArgumentException("The image signature does not match its filename and content type");
        }
    }

    private static byte[] canonicalizeImage(String extension, byte[] content) {
        try (ImageInputStream imageInput = new MemoryCacheImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("The uploaded content is not a supported image");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                long pixels = (long) width * height;
                if (width <= 0 || height <= 0 || width > MAX_IMAGE_DIMENSION
                        || height > MAX_IMAGE_DIMENSION || pixels > MAX_IMAGE_PIXELS) {
                    throw new IllegalArgumentException("The image dimensions are not allowed");
                }

                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw new IllegalArgumentException("The uploaded image could not be decoded");
                }
                BufferedImage oriented = "jpg".equals(extension) || "jpeg".equals(extension)
                        ? applyExifOrientation(decoded, readExifOrientation(content))
                        : decoded;
                BufferedImage canonical = toCanonicalColorModel(oriented, extension);
                ByteArrayOutputStream output = new ByteArrayOutputStream(
                        Math.min(content.length, (int) MAX_UPLOAD_SIZE));
                String format = "jpeg".equals(extension) ? "jpg" : extension;
                if (!ImageIO.write(canonical, format, output)) {
                    throw new IllegalArgumentException("The uploaded image format cannot be encoded safely");
                }
                return output.toByteArray();
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }
            throw new IllegalArgumentException("The uploaded image is corrupt", e);
        }
    }

    private static BufferedImage toCanonicalColorModel(BufferedImage source, String extension) {
        boolean keepAlpha = "png".equals(extension) && source.getColorModel().hasAlpha();
        int imageType = keepAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        if (source.getType() == imageType) {
            return source;
        }
        BufferedImage canonical = new BufferedImage(source.getWidth(), source.getHeight(), imageType);
        java.awt.Graphics2D graphics = canonical.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return canonical;
    }

    private static int readExifOrientation(byte[] content) {
        if (!startsWith(content, 0xFF, 0xD8, 0xFF)) {
            return 1;
        }

        int offset = 2;
        while (offset + 4 <= content.length) {
            if ((content[offset] & 0xFF) != 0xFF) {
                return 1;
            }
            int marker = content[offset + 1] & 0xFF;
            if (marker == 0xDA || marker == 0xD9) {
                return 1;
            }
            int segmentLength = readUnsignedShort(content, offset + 2, false);
            if (segmentLength < 2 || offset + 2L + segmentLength > content.length) {
                return 1;
            }

            int payloadOffset = offset + 4;
            int payloadLength = segmentLength - 2;
            if (marker == 0xE1 && payloadLength >= 14
                    && matchesAscii(content, payloadOffset, "Exif\0\0")) {
                return parseTiffOrientation(content, payloadOffset + 6,
                        payloadOffset + payloadLength);
            }
            offset += 2 + segmentLength;
        }
        return 1;
    }

    private static int parseTiffOrientation(byte[] content, int tiffOffset, int limit) {
        if (tiffOffset + 8 > limit) {
            return 1;
        }
        boolean littleEndian;
        if (content[tiffOffset] == 'I' && content[tiffOffset + 1] == 'I') {
            littleEndian = true;
        } else if (content[tiffOffset] == 'M' && content[tiffOffset + 1] == 'M') {
            littleEndian = false;
        } else {
            return 1;
        }
        if (readUnsignedShort(content, tiffOffset + 2, littleEndian) != 42) {
            return 1;
        }

        long ifdRelativeOffset = readUnsignedInt(content, tiffOffset + 4, littleEndian);
        long ifdOffsetLong = tiffOffset + ifdRelativeOffset;
        if (ifdOffsetLong < tiffOffset || ifdOffsetLong + 2 > limit) {
            return 1;
        }
        int ifdOffset = (int) ifdOffsetLong;
        int entryCount = readUnsignedShort(content, ifdOffset, littleEndian);
        int entryOffset = ifdOffset + 2;
        for (int index = 0; index < entryCount; index++, entryOffset += 12) {
            if (entryOffset + 12 > limit) {
                return 1;
            }
            int tag = readUnsignedShort(content, entryOffset, littleEndian);
            int type = readUnsignedShort(content, entryOffset + 2, littleEndian);
            long count = readUnsignedInt(content, entryOffset + 4, littleEndian);
            if (tag == 0x0112 && type == 3 && count == 1) {
                int orientation = readUnsignedShort(content, entryOffset + 8, littleEndian);
                return orientation >= 1 && orientation <= 8 ? orientation : 1;
            }
        }
        return 1;
    }

    private static BufferedImage applyExifOrientation(BufferedImage source, int orientation) {
        if (orientation == 1) {
            return source;
        }

        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        boolean swapDimensions = orientation >= 5;
        BufferedImage result = new BufferedImage(
                swapDimensions ? sourceHeight : sourceWidth,
                swapDimensions ? sourceWidth : sourceHeight,
                BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < sourceHeight; y++) {
            for (int x = 0; x < sourceWidth; x++) {
                int destinationX;
                int destinationY;
                switch (orientation) {
                    case 2:
                        destinationX = sourceWidth - 1 - x;
                        destinationY = y;
                        break;
                    case 3:
                        destinationX = sourceWidth - 1 - x;
                        destinationY = sourceHeight - 1 - y;
                        break;
                    case 4:
                        destinationX = x;
                        destinationY = sourceHeight - 1 - y;
                        break;
                    case 5:
                        destinationX = y;
                        destinationY = x;
                        break;
                    case 6:
                        destinationX = sourceHeight - 1 - y;
                        destinationY = x;
                        break;
                    case 7:
                        destinationX = sourceHeight - 1 - y;
                        destinationY = sourceWidth - 1 - x;
                        break;
                    case 8:
                        destinationX = y;
                        destinationY = sourceWidth - 1 - x;
                        break;
                    default:
                        destinationX = x;
                        destinationY = y;
                        break;
                }
                result.setRGB(destinationX, destinationY, source.getRGB(x, y));
            }
        }
        return result;
    }

    private static int readUnsignedShort(byte[] content, int offset, boolean littleEndian) {
        int first = content[offset] & 0xFF;
        int second = content[offset + 1] & 0xFF;
        return littleEndian ? first | second << 8 : first << 8 | second;
    }

    private static long readUnsignedInt(byte[] content, int offset, boolean littleEndian) {
        long first = content[offset] & 0xFFL;
        long second = content[offset + 1] & 0xFFL;
        long third = content[offset + 2] & 0xFFL;
        long fourth = content[offset + 3] & 0xFFL;
        return littleEndian
                ? first | second << 8 | third << 16 | fourth << 24
                : first << 24 | second << 16 | third << 8 | fourth;
    }

    private static boolean matchesAscii(byte[] content, int offset, String expected) {
        if (offset < 0 || offset + expected.length() > content.length) {
            return false;
        }
        for (int index = 0; index < expected.length(); index++) {
            if ((content[offset + index] & 0xFF) != expected.charAt(index)) {
                return false;
            }
        }
        return true;
    }

    private static boolean startsWith(byte[] content, int... signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((content[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeContentType(String contentType) {
        int parameter = contentType.indexOf(';');
        return (parameter >= 0 ? contentType.substring(0, parameter) : contentType)
                .trim().toLowerCase(Locale.ROOT);
    }

    private void compensateUploadedObject(FileUploadVO uploadedFile, Exception primaryFailure) {
        try {
            deleteFileToS3(uploadedFile);
        } catch (IOException | RuntimeException cleanupFailure) {
            primaryFailure.addSuppressed(cleanupFailure);
            LOG.warn("Failed to remove an uploaded object after database persistence failed");
        }
    }

    private void compensateCopiedObject(S3Client s3Client, String destinationKey,
            Exception primaryFailure) {
        try {
            deleteObject(s3Client, bucket2, destinationKey);
        } catch (RuntimeException cleanupFailure) {
            primaryFailure.addSuppressed(cleanupFailure);
            LOG.warn("Failed to remove a copied object after database persistence failed");
        }
    }

    private void restoreStoredFileState(FileUploadVO inVO, int checked, String url) {
        inVO.setChecked(checked);
        inVO.setUrl(url);
    }

    protected void deleteFileToS3(FileUploadVO inVO) throws IOException {
        String sourceBucket = usesReviewedBucket(inVO.getChecked()) ? bucket2 : bucket;
        S3Client s3Client = createS3Client();
        try {
            deleteObject(s3Client, sourceBucket, inVO.getName());
            LOG.debug("Stored image deleted");
        } finally {
            try {
                s3Client.close();
            } catch (RuntimeException closeFailure) {
                LOG.warn("Failed to close the image storage client");
            }
        }
    }

    private void validateStoredFile(FileUploadVO inVO) {
        if (inVO == null || inVO.getName() == null || inVO.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("A stored filename is required");
        }
    }

    private boolean usesReviewedBucket(int checked) {
        return checked == 1 || checked == PENDING_DELETE_REVIEWED;
    }

    private boolean isPendingDelete(int checked) {
        return checked == PENDING_DELETE_INCOMING || checked == PENDING_DELETE_REVIEWED;
    }

    protected String buildObjectUrl(S3Client s3Client, String targetBucket, String objectKey) {
        return s3Client.utilities().getUrl(
                builder -> builder.bucket(targetBucket).key(objectKey)).toExternalForm();
    }

    protected S3Client createS3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    private void deleteObject(S3Client s3Client, String targetBucket, String objectKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(targetBucket)
                .key(objectKey)
                .build());
    }

}
