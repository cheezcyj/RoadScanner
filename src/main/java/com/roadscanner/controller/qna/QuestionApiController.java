package com.roadscanner.controller.qna;

import com.roadscanner.cmn.AppLogger;
import com.roadscanner.domain.upload.FileUploadVO;
import com.roadscanner.domain.qna.QuestionCategory;
import com.roadscanner.domain.user.MemberVO;
import com.roadscanner.dto.QuestionResponseDTO;
import com.roadscanner.dto.QuestionSaveRequestDTO;
import com.roadscanner.dto.QuestionUpdateRequestDTO;
import com.roadscanner.service.qna.QuestionService;
import com.roadscanner.service.upload.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;

@RequiredArgsConstructor
@RestController
public class QuestionApiController implements AppLogger {

    private static final int ADMIN_GRADE = 2;
    private static final int QUESTION_ATTACHMENT_CATEGORY = 40;

    private final QuestionService questionService;
    private final FileUploadService fileUploadService;

    /** Text-only compatibility endpoint; attachments require the multipart contract. */
    @PostMapping(value = "/api/qna/save", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Long save(@Valid @RequestBody QuestionSaveRequestDTO dto,
                     @SessionAttribute("user") MemberVO user) throws SQLException {
        prepareSave(dto, user, false);
        if (dto.getIdx() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "JSON question requests cannot attach uploaded files");
        }
        return questionService.save(dto);
    }

    @PostMapping(value = "/api/qna/inquiries", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Long saveInquiry(@Valid @RequestBody QuestionSaveRequestDTO dto,
                            @SessionAttribute("user") MemberVO user) throws SQLException {
        prepareSave(dto, user, true);
        if (dto.getIdx() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "JSON inquiry requests cannot attach uploaded files");
        }
        return questionService.save(dto);
    }

    @PostMapping(value = "/api/qna/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Long saveMultipart(@Valid @ModelAttribute QuestionSaveRequestDTO dto,
                              @RequestParam(value = "fileUpload", required = false) MultipartFile file,
                              @SessionAttribute("user") MemberVO user) throws SQLException, IOException {
        return saveMultipart(dto, file, user, false);
    }

    @PostMapping(value = "/api/qna/inquiries", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Long saveInquiryMultipart(@Valid @ModelAttribute QuestionSaveRequestDTO dto,
                                     @RequestParam(value = "fileUpload", required = false) MultipartFile file,
                                     @SessionAttribute("user") MemberVO user)
            throws SQLException, IOException {
        return saveMultipart(dto, file, user, true);
    }

    private Long saveMultipart(QuestionSaveRequestDTO dto, MultipartFile file,
                               MemberVO user, boolean inquiry) throws SQLException, IOException {
        prepareSave(dto, user, inquiry);
        // A multipart client cannot bind an attachment chosen outside this request.
        dto.setIdx(null);

        FileUploadVO uploaded = null;
        if (hasFile(file)) {
            uploaded = uploadQuestionAttachment(file, user);
            dto.setIdx((long) uploaded.getIdx());
        }

        try {
            Long savedQuestion = questionService.save(dto);
            if (savedQuestion == null || savedQuestion <= 0) {
                throw new IllegalStateException("Question persistence did not return a valid identifier");
            }
            return savedQuestion;
        } catch (RuntimeException | Error failure) {
            compensateNewAttachment(uploaded, failure);
            throw failure;
        }
    }

    @GetMapping("/api/qna/{no}")
    public QuestionResponseDTO findByNo(@PathVariable Long no,
                                        @SessionAttribute("user") MemberVO user) {
        QuestionResponseDTO question = findQuestion(no);
        assertCanView(question, user);
        return question;
    }

    /** Text-only compatibility endpoint that always keeps the current attachment. */
    @PutMapping(value = "/api/qna/{no}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Long update(@PathVariable Long no, @Valid @RequestBody QuestionUpdateRequestDTO dto,
                       @SessionAttribute("user") MemberVO user) throws SQLException {
        QuestionResponseDTO question = findQuestion(no);
        assertCanModify(question, user);
        prepareUpdate(dto, question);

        if (dto.getIdx() != null && !Objects.equals(dto.getIdx(), question.getIdx())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "JSON question requests cannot replace attachments");
        }
        // JSON compatibility requests may edit text fields only. Attachment removal
        // and replacement require the multipart endpoint's explicit action contract.
        dto.setIdx(question.getIdx());
        Long updatedQuestion = questionService.update(no, dto);
        if (updatedQuestion == null || updatedQuestion <= 0) {
            throw new IllegalStateException("Question persistence did not return a valid identifier");
        }
        return updatedQuestion;
    }

    /**
     * Atomicity-oriented endpoint used by qna.js. A replacement is uploaded first,
     * the question is persisted next, and the old attachment is deleted last.
     */
    @PostMapping(value = "/api/qna/{no}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Long updateMultipart(@PathVariable Long no,
                                @Valid @ModelAttribute QuestionUpdateRequestDTO dto,
                                @RequestParam("attachmentAction") AttachmentAction attachmentAction,
                                @RequestParam(value = "fileUpload", required = false) MultipartFile file,
                                @SessionAttribute("user") MemberVO user) throws SQLException, IOException {
        QuestionResponseDTO question = findQuestion(no);
        assertCanModify(question, user);
        prepareUpdate(dto, question);
        validateAttachmentAction(attachmentAction, file);

        FileUploadVO oldAttachment = findReferencedAttachment(question);
        FileUploadVO newAttachment = null;
        switch (attachmentAction) {
            case KEEP:
                dto.setIdx(question.getIdx());
                break;
            case REMOVE:
                dto.setIdx(null);
                break;
            case REPLACE:
                newAttachment = uploadQuestionAttachment(file, user);
                dto.setIdx((long) newAttachment.getIdx());
                break;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid attachment action");
        }

        Long updatedQuestion;
        try {
            updatedQuestion = questionService.update(no, dto);
            if (updatedQuestion == null || updatedQuestion <= 0) {
                throw new IllegalStateException("Question persistence did not return a valid identifier");
            }
        } catch (RuntimeException | Error failure) {
            compensateNewAttachment(newAttachment, failure);
            throw failure;
        }

        if (attachmentAction != AttachmentAction.KEEP) {
            deleteOldAttachmentAfterPersistence(oldAttachment);
        }
        return updatedQuestion;
    }

    @DeleteMapping("/api/qna/{no}")
    public Long delete(@PathVariable Long no,
                       @SessionAttribute("user") MemberVO user) throws SQLException {
        QuestionResponseDTO question = findQuestion(no);
        assertCanModify(question, user);
        FileUploadVO oldAttachment = findReferencedAttachment(question);

        Long deletedQuestion = questionService.delete(no);
        if (deletedQuestion == null || deletedQuestion <= 0) {
            throw new IllegalStateException("Question deletion did not return a valid identifier");
        }
        deleteOldAttachmentAfterPersistence(oldAttachment);
        return deletedQuestion;
    }

    private void prepareSave(QuestionSaveRequestDTO dto, MemberVO user, boolean inquiry) {
        dto.setId(user.getId());
        if (inquiry) {
            dto.setCategory(QuestionCategory.INQUIRY_WAITING);
            return;
        }
        boolean adminNotice = isAdmin(user)
                && Integer.valueOf(QuestionCategory.NOTICE).equals(dto.getCategory());
        dto.setCategory(adminNotice ? QuestionCategory.NOTICE : QuestionCategory.BOARD_POST);
    }

    private void prepareUpdate(QuestionUpdateRequestDTO dto, QuestionResponseDTO question) {
        // Board kind and inquiry status are server-owned. AnswerService is the only
        // component allowed to transition an inquiry between waiting and answered.
        dto.setCategory(question.getCategory());
    }

    private QuestionResponseDTO findQuestion(Long no) {
        QuestionResponseDTO question = questionService.findByNo(no);
        if (question == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
        }
        return question;
    }

    private void assertCanModify(QuestionResponseDTO question, MemberVO user) {
        if (!isAdmin(user) && !question.getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Question access denied");
        }
    }

    private void assertCanView(QuestionResponseDTO question, MemberVO user) {
        if (QuestionCategory.isBoard(question.getCategory())) {
            return;
        }
        if (!QuestionCategory.isInquiry(question.getCategory())
                || (!isAdmin(user) && !question.getId().equals(user.getId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Inquiry access denied");
        }
    }

    private boolean isAdmin(MemberVO user) {
        return user.getGrade() == ADMIN_GRADE;
    }

    private FileUploadVO uploadQuestionAttachment(MultipartFile file, MemberVO user)
            throws SQLException, IOException {
        FileUploadVO upload = new FileUploadVO();
        upload.setId(user.getId());
        upload.setCategory(QUESTION_ATTACHMENT_CATEGORY);

        String savedName = fileUploadService.doSave(file, upload);
        if (savedName == null || savedName.trim().isEmpty() || "0".equals(savedName)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Attachment storage failed");
        }
        upload.setName(savedName);

        try {
            FileUploadVO stored = fileUploadService.doSelectOne(upload);
            if (stored == null || stored.getIdx() <= 0
                    || stored.getCategory() != QUESTION_ATTACHMENT_CATEGORY
                    || !user.getId().equals(stored.getId())) {
                throw new IllegalStateException("Stored attachment metadata is inconsistent");
            }
            return stored;
        } catch (SQLException | RuntimeException | Error failure) {
            compensateNewAttachment(upload, failure);
            throw failure;
        }
    }

    private FileUploadVO findReferencedAttachment(QuestionResponseDTO question) throws SQLException {
        Long attachmentId = question.getIdx();
        if (attachmentId == null || attachmentId <= 0 || attachmentId > Integer.MAX_VALUE) {
            return null;
        }

        FileUploadVO lookup = new FileUploadVO();
        lookup.setIdx(attachmentId.intValue());
        FileUploadVO attachment = fileUploadService.doSelectOne(lookup);
        if (attachment == null || attachment.getCategory() != QUESTION_ATTACHMENT_CATEGORY) {
            LOG.warn("Question attachment metadata is missing or inconsistent; cleanup was skipped");
            return null;
        }
        return attachment;
    }

    private void validateAttachmentAction(AttachmentAction action, MultipartFile file) {
        if (action == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "An attachment action is required");
        }
        boolean hasFile = hasFile(file);
        if (action == AttachmentAction.REPLACE && !hasFile) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A replacement attachment is required");
        }
        if (action != AttachmentAction.REPLACE && hasFile) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "An attachment is only allowed with REPLACE");
        }
    }

    private boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private void compensateNewAttachment(FileUploadVO attachment, Throwable primaryFailure) {
        if (attachment == null) {
            return;
        }
        try {
            int deleted = fileUploadService.doDelete(attachment);
            if (deleted != 1) {
                primaryFailure.addSuppressed(
                        new IllegalStateException("Attachment compensation did not delete one record"));
                LOG.warn("Attachment compensation did not delete one record");
            }
        } catch (SQLException | IOException | RuntimeException cleanupFailure) {
            primaryFailure.addSuppressed(cleanupFailure);
            LOG.warn("Failed to compensate a newly uploaded question attachment", cleanupFailure);
        }
    }

    private void deleteOldAttachmentAfterPersistence(FileUploadVO attachment) {
        if (attachment == null) {
            return;
        }
        try {
            if (questionService.countByAttachmentId((long) attachment.getIdx()) > 0) {
                LOG.debug("Question attachment is still referenced; storage cleanup was skipped");
                return;
            }
            if (fileUploadService.doDelete(attachment) != 1) {
                LOG.warn("Old question attachment cleanup did not delete one record");
            }
        } catch (SQLException | IOException | RuntimeException cleanupFailure) {
            // The question is already in a valid state. Do not make the client retry and
            // potentially create another upload; retain the old object as a recoverable orphan.
            LOG.warn("Question was persisted, but old attachment cleanup failed", cleanupFailure);
        }
    }

    public enum AttachmentAction {
        KEEP,
        REMOVE,
        REPLACE
    }
}
