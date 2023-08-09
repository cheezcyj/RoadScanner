package com.roadscanner.dao.qna;

import com.roadscanner.cmn.BaseRepository;
import com.roadscanner.domain.qna.QuestionVO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionDAO extends BaseRepository<QuestionVO> {

    /**
     * 게시판, 댓글 CRUD가 겹치는 것이 많음 => WorkDiv의 사용 => 재사용성을 올림
     * 공통작업을 WorkdDiv 참고 => WorkdDiv => BaseRepository로 수정
     * 게시판에 추가로 필요한 메소드명만 정의할 것(예, 조회수, 검색)
     */

}
