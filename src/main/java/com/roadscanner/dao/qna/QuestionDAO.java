package com.roadscanner.dao.qna;

import com.roadscanner.cmn.BaseRepository;
import com.roadscanner.domain.qna.QuestionVO;
import com.roadscanner.dto.PaginationDTO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionDAO extends BaseRepository<QuestionVO> {

    /**
     * 게시판, 댓글 CRUD가 겹치는 것이 많음 => WorkDiv의 사용 => 재사용성을 올림
     * 공통작업을 WorkdDiv 참고 => WorkdDiv => BaseRepository로 수정
     * 게시판에 추가로 필요한 메소드명만 정의할 것(예, 조회수, 검색)
     */

    // findAll을 수정하면 되는거 아닌가 했지만 페이징을 위해 따로 구현하는게 더 좋은 접근방식 이라고 한다..
    List<QuestionVO> findBoardWithPaging(
            @Param("pagination") PaginationDTO pagination,
            @Param("category") Integer category,
            @Param("searchType") String searchType,
            @Param("keywordPattern") String keywordPattern);

    List<QuestionVO> findInquiriesWithPaging(
            @Param("pagination") PaginationDTO pagination,
            @Param("category") Integer category,
            @Param("searchType") String searchType,
            @Param("keywordPattern") String keywordPattern);

    List<QuestionVO> findInquiriesByAuthorWithPaging(
            @Param("id") String id,
            @Param("pagination") PaginationDTO pagination,
            @Param("category") Integer category,
            @Param("searchType") String searchType,
            @Param("keywordPattern") String keywordPattern);

    QuestionVO findBoardByNo(Long no);

    QuestionVO findInquiryByNo(Long no);

    QuestionVO findInquiryByNoAndAuthor(@Param("no") Long no, @Param("id") String id);

    int countBoard(
            @Param("category") Integer category,
            @Param("searchType") String searchType,
            @Param("keywordPattern") String keywordPattern);

    int countInquiries(
            @Param("category") Integer category,
            @Param("searchType") String searchType,
            @Param("keywordPattern") String keywordPattern);

    int countInquiriesByAuthor(
            @Param("id") String id,
            @Param("category") Integer category,
            @Param("searchType") String searchType,
            @Param("keywordPattern") String keywordPattern);

    int countByAttachmentId(Long attachmentId);

    void increaseViews(Long no); // 조회수 증가 메서드

    int transitionCategory(@Param("no") Long no,
                           @Param("expectedCategory") int expectedCategory,
                           @Param("newCategory") int newCategory);
}
