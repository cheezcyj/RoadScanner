package com.roadscanner.dao.qna;

import com.roadscanner.cmn.BaseRepository;
import com.roadscanner.domain.qna.QuestionVO;
import com.roadscanner.dto.qna.PaginationDTO;
import com.roadscanner.dto.qna.QuestionSearchCond;
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


    /**
     * 전체 게시글 수 반환
     * @param searchCond
     */
    int countQuestions(QuestionSearchCond searchCond);


    /**
     * 조회수 증가
     * @param no
     */
    void increaseViews(Long no);


    /**
     * 질문글 분류 변경
     * @param no
     */
    void updateCategory(Long no);


    /**
     * 페이징과 검색 조건을 적용하여 데이터 조회
     * @param pagination 페이징 정보
     * @param searchCond 검색조건, 검색어
     */
    List<QuestionVO> findAll(@Param("pagination") PaginationDTO pagination, @Param("searchCond") QuestionSearchCond searchCond);


    /**
     * 공지사항 조회
     * @return
     */
    List<QuestionVO> findNotice();

    /**
     * 내 글 보기
     * @param id
     * @param pagination
     * @param searchCond
     * @return
     */
    List<QuestionVO> findMyQuestion(@Param("id") String id, @Param("pagination") PaginationDTO pagination, @Param("searchCond") QuestionSearchCond searchCond);

    /**
     * 내 글 개수 세기
     * @param searchCond
     * @return
     */
    int countMyQuestions(QuestionSearchCond searchCond);


}
