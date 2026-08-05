package com.roadscanner.service.qna;

import com.roadscanner.dao.qna.QuestionDAO;
import com.roadscanner.cmn.exception.InvalidOperationException;
import com.roadscanner.cmn.exception.ResourceNotFoundException;
import com.roadscanner.cmn.validation.RichTextSanitizer;
import com.roadscanner.domain.qna.QuestionCategory;
import com.roadscanner.domain.qna.QuestionVO;
import com.roadscanner.dto.PaginationDTO;
import com.roadscanner.dto.QuestionListResponseDTO;
import com.roadscanner.dto.QuestionResponseDTO;
import com.roadscanner.dto.QuestionSaveRequestDTO;
import com.roadscanner.dto.QuestionUpdateRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class QuestionServiceImpl implements QuestionService {

    private static final String SEARCH_BOTH = "both";
    private static final String SEARCH_TITLE = "title";
    private static final String SEARCH_CONTENT = "content";
    private static final int MAX_KEYWORD_LENGTH = 100;

    private final QuestionDAO questionDAO;

    // 전체조회 + 페이징 추가 정리 필요
    @Override
    public List<QuestionListResponseDTO> findBoardWithPaging(PaginationDTO pagination,
                                                              Integer category,
                                                              String searchType,
                                                              String keyword) {
        return toListResponse(questionDAO.findBoardWithPaging(
                pagination,
                normalizeBoardCategory(category),
                normalizeSearchType(searchType),
                toLikePattern(keyword)));
    }

    @Override
    public List<QuestionListResponseDTO> findInquiriesWithPaging(PaginationDTO pagination,
                                                                  Integer category,
                                                                  String searchType,
                                                                  String keyword) {
        return toListResponse(questionDAO.findInquiriesWithPaging(
                pagination,
                normalizeInquiryCategory(category),
                normalizeSearchType(searchType),
                toLikePattern(keyword)));
    }

    @Override
    public List<QuestionListResponseDTO> findInquiriesByAuthorWithPaging(String id,
                                                                          PaginationDTO pagination,
                                                                          Integer category,
                                                                          String searchType,
                                                                          String keyword) {
        return toListResponse(questionDAO.findInquiriesByAuthorWithPaging(
                id,
                pagination,
                normalizeInquiryCategory(category),
                normalizeSearchType(searchType),
                toLikePattern(keyword)));
    }

    private List<QuestionListResponseDTO> toListResponse(List<QuestionVO> questions) {
        List<QuestionListResponseDTO> dto = new ArrayList<>();

        for (QuestionVO question : questions) {
            dto.add(new QuestionListResponseDTO(question));
        }
        return dto;
    }

    private QuestionResponseDTO toResponse(QuestionVO question) {
        return question == null ? null : new QuestionResponseDTO(question);
    }

    @Override
    public Long save(QuestionSaveRequestDTO dto) {

        // DTO(사용자가 제공한 정보를 통해) 질문 VO 객체 생성
        QuestionVO vo = dto.toEntity();
        if (!QuestionCategory.isCreatable(vo.getCategory())) {
            throw new InvalidOperationException("Invalid question category for creation");
        }
        vo.setContent(RichTextSanitizer.sanitize(vo.getContent()));
        // DAO를 통해 데이터베이스에 질문 등록 잘지내시나요?
        return questionDAO.save(vo);
    }

    @Override
    public QuestionResponseDTO findByNo(Long no) {
        return toResponse(questionDAO.findByNo(no));
    }

    @Override
    public QuestionResponseDTO findBoardByNo(Long no) {
        return toResponse(questionDAO.findBoardByNo(no));
    }

    @Override
    public QuestionResponseDTO findInquiryByNo(Long no) {
        return toResponse(questionDAO.findInquiryByNo(no));
    }

    @Override
    public QuestionResponseDTO findInquiryByNoAndAuthor(Long no, String id) {
        return toResponse(questionDAO.findInquiryByNoAndAuthor(no, id));
    }

    @Override
    public Long update(Long no, QuestionUpdateRequestDTO dto) {
        // findById 메서드를 완성 시켜야함 단건 조회후 수정
        QuestionVO vo = questionDAO.findByNo(no);
        if (vo == null) {
            throw new ResourceNotFoundException("Question not found");
        }
        if (!QuestionCategory.isSupported(vo.getCategory())) {
            throw new InvalidOperationException("Invalid persisted question category");
        }
        vo.update(vo.getCategory(), dto.getTitle(), dto.getIdx(),
                RichTextSanitizer.sanitize(dto.getContent()));
        if (questionDAO.update(vo) != 1) {
            throw new IllegalStateException("Question update did not affect exactly one row");
        }
        return no;
    }

    @Override
    public Long delete(Long no) {
        if (questionDAO.delete(no) != 1) {
            throw new IllegalStateException("Question deletion did not affect exactly one row");
        }
        return no;
    }

    @Override
    public int countBoard(Integer category, String searchType, String keyword) {
        return questionDAO.countBoard(
                normalizeBoardCategory(category),
                normalizeSearchType(searchType),
                toLikePattern(keyword));
    }

    @Override
    public int countInquiries(Integer category, String searchType, String keyword) {
        return questionDAO.countInquiries(
                normalizeInquiryCategory(category),
                normalizeSearchType(searchType),
                toLikePattern(keyword));
    }

    @Override
    public int countInquiriesByAuthor(String id, Integer category,
                                      String searchType, String keyword) {
        return questionDAO.countInquiriesByAuthor(
                id,
                normalizeInquiryCategory(category),
                normalizeSearchType(searchType),
                toLikePattern(keyword));
    }

    @Override
    public int countByAttachmentId(Long attachmentId) {
        return questionDAO.countByAttachmentId(attachmentId);
    }

    @Override
    public void increaseViews(Long no) {
        questionDAO.increaseViews(no);
    }

    private Integer normalizeBoardCategory(Integer code) {
        return QuestionCategory.isBoard(code) ? code : null;
    }

    private Integer normalizeInquiryCategory(Integer code) {
        return QuestionCategory.isInquiry(code) ? code : null;
    }

    private String normalizeSearchType(String searchType) {
        if (SEARCH_TITLE.equals(searchType) || SEARCH_CONTENT.equals(searchType)) {
            return searchType;
        }
        return SEARCH_BOTH;
    }

    private String toLikePattern(String keyword) {
        if (keyword == null) {
            return null;
        }

        String normalized = keyword.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.codePointCount(0, normalized.length()) > MAX_KEYWORD_LENGTH) {
            normalized = normalized.substring(0, normalized.offsetByCodePoints(0, MAX_KEYWORD_LENGTH));
        }

        String escaped = normalized
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

}
