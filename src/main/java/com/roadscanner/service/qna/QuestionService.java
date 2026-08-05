package com.roadscanner.service.qna;

import com.roadscanner.dto.PaginationDTO;
import com.roadscanner.dto.QuestionListResponseDTO;
import com.roadscanner.dto.QuestionResponseDTO;
import com.roadscanner.dto.QuestionSaveRequestDTO;
import com.roadscanner.dto.QuestionUpdateRequestDTO;

import java.util.List;

public interface QuestionService{

    /**
     * 게시글 작성
     */

    Long save(QuestionSaveRequestDTO dto);

    QuestionResponseDTO findByNo(Long no);

    QuestionResponseDTO findBoardByNo(Long no);

    QuestionResponseDTO findInquiryByNo(Long no);

    QuestionResponseDTO findInquiryByNoAndAuthor(Long no, String id);

    Long update(Long no, QuestionUpdateRequestDTO dto);

    Long delete(Long no);

    List<QuestionListResponseDTO> findBoardWithPaging(PaginationDTO pagination,
                                                       Integer category,
                                                       String searchType,
                                                       String keyword);

    int countBoard(Integer category, String searchType, String keyword);

    List<QuestionListResponseDTO> findInquiriesWithPaging(PaginationDTO pagination,
                                                           Integer category,
                                                           String searchType,
                                                           String keyword);

    int countInquiries(Integer category, String searchType, String keyword);

    List<QuestionListResponseDTO> findInquiriesByAuthorWithPaging(String id,
                                                                   PaginationDTO pagination,
                                                                   Integer category,
                                                                   String searchType,
                                                                   String keyword);

    int countInquiriesByAuthor(String id, Integer category, String searchType, String keyword);

    // 안녕하세요. 저는 페이징을 위해 태어났어요.
    int countByAttachmentId(Long attachmentId);

    void increaseViews(Long no);

}
