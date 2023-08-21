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
    public List<QuestionListResponseDTO> findAll(PaginationDTO pagination, QuestionSearchCond questionSearch) {
        List<QuestionVO> questionList = questionDAO.findAll(pagination, questionSearch);
        List<QuestionListResponseDTO> dto = new ArrayList<>();

        for (QuestionVO question : questionList) {
            dto.add(new QuestionListResponseDTO(question));
        }

        return dto;
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
            vo.setOriginalFilename(attachFile.getUploadFileName());
            vo.setStoreFilename(attachFile.getStoreFileName());
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
    public Long update(Long no, QuestionUpdateRequestDTO dto) {
        // findById 메서드를 완성 시켜야함 단건 조회후 수정
        QuestionVO vo = questionDAO.findByNo(no);
        questionDAO.update(vo);
        return no;
    }
    @Override
    public Long delete(Long no) {
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

    // 게시글 분류(category) 변경
//    @Override
//    public Long updateCategory(Long no) {
//        questionDAO.updateCategory(no);
//        return no;
//    }

}
