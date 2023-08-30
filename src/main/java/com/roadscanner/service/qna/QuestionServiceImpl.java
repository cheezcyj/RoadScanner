package com.roadscanner.service.qna;

import com.roadscanner.cmn.AmazonS3Store;
import com.roadscanner.cmn.PcwkLogger;
import com.roadscanner.dao.qna.QuestionDAO;
import com.roadscanner.domain.UploadFile;
import com.roadscanner.domain.qna.QuestionVO;
import com.roadscanner.dto.qna.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 사용자의 문의 게시글에 관련된 서비스를 제공(Question)
 */
@RequiredArgsConstructor
@Service
public class QuestionServiceImpl implements QuestionService, PcwkLogger {

    private final AmazonS3Store fileStore;
    private final QuestionDAO questionDAO;

    @Override
    public List<QuestionListResponseDTO> findMyQuestion(String id, PaginationDTO pagination, QuestionSearchCond searchCond) {
        List<QuestionVO> questionList = questionDAO.findMyQuestion(id, pagination, searchCond);
        List<QuestionListResponseDTO> dtoList = new ArrayList<>();

        for (QuestionVO question : questionList) {
            dtoList.add(new QuestionListResponseDTO(question));
        }

        return dtoList;
    }

    @Override
    public int countMyQuestions(QuestionSearchCond searchCond) {
            return questionDAO.countMyQuestions(searchCond);

    }

    @Override
    public List<QuestionListResponseDTO> findNotice() {
        List<QuestionVO> questionNoticeList = questionDAO.findNotice();
        List<QuestionListResponseDTO> dtoList = new ArrayList<>();

        for (QuestionVO question : questionNoticeList) {
            dtoList.add(new QuestionListResponseDTO(question));
        }

        return dtoList;
    }

    @Override
    public List<QuestionListResponseDTO> findAll(PaginationDTO pagination, QuestionSearchCond questionSearch) {
        List<QuestionVO> questionList = questionDAO.findAll(pagination, questionSearch);
        List<QuestionListResponseDTO> dtoList = new ArrayList<>();

        for (QuestionVO question : questionList) {
            dtoList.add(new QuestionListResponseDTO(question));
        }

        return dtoList;
    }

    /**
     * 컨트롤러에서 받은 사용자의 요청을 확인하고 파일을 업로드 했는지 여부를 먼저 검사함
     * 업로드 되어있다면 VO에 값을 넣어줌.
     * @param request
     * @return
     * @throws IOException
     */

    @Override
    @Transactional
    public void save(QuestionSaveRequestDTO request) throws IOException {

        UploadFile attachFile = null;
        if (request.getAttachFile() != null && !request.getAttachFile().isEmpty()) {
            attachFile = fileStore.storeFile(request.getAttachFile());
        }

        QuestionVO vo = new QuestionVO();
        vo.setCategory(request.getCategory());
        vo.setId(request.getId());
        vo.setTitle(request.getTitle());
        vo.setContent(request.getContent());

        if (attachFile != null) {
            vo.setOriginalFilename(attachFile.getUploadFilename());
            vo.setStoreFilename(attachFile.getStoreFilename());
            vo.setImageUrl(attachFile.getUrl());
        }

        LOG.info("vo={}", vo);
        LOG.info("attachFile={}", attachFile);

        questionDAO.save(vo);

    }

    @Override
    @Transactional
    public QuestionResponseDTO findByNo(Long no) {
        QuestionVO vo = questionDAO.findByNo(no);
        return new QuestionResponseDTO(vo);
    }

    @Override
    @Transactional
    public Long update(Long no, QuestionUpdateRequestDTO request) throws IOException {

        QuestionVO question = questionDAO.findByNo(no);

        LOG.debug("데이터베이스에 있던 게시글 ={}", question);

        if (request.isFileChanged()) {
            fileStore.deleteFile(question.getStoreFilename());
            if (request.getAttachFile() == null) {
                question.setOriginalFilename(null);
                question.setStoreFilename(null);
                question.setImageUrl(null);
            } else if (request.getAttachFile() != null && !request.getAttachFile().isEmpty()) {
                UploadFile attachFile;
                attachFile = fileStore.storeFile(request.getAttachFile());
                question.setOriginalFilename(attachFile.getUploadFilename());
                question.setStoreFilename(attachFile.getStoreFilename());
                question.setImageUrl(attachFile.getUrl());
            }
        }

        LOG.debug("사용자 요청={}", request);
        // 제목 및 내용 수정
        question.setTitle(request.getTitle());
        question.setContent(request.getContent());
        LOG.debug("데이터베이스에 수정될 내용={}", question);
        questionDAO.update(question);
        return no;
    }

    @Override
    public Long delete(Long no) {
        QuestionVO findByQuestion = questionDAO.findByNo(no);
        fileStore.deleteFile(findByQuestion.getStoreFilename());
        questionDAO.delete(no);
        return no;
    }

    @Override
    public int countQuestions(QuestionSearchCond searchCond) {
        return questionDAO.countQuestions(searchCond);
    }

    @Override
    public void increaseViews(Long no) {
        questionDAO.increaseViews(no);
    }

}
