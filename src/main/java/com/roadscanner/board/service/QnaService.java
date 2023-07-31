package com.roadscanner.board.service;

import java.sql.SQLException;
import java.util.List;

import com.roadscanner.board.domain.Qna;

public interface QnaService {
    void createQna(Qna qna) throws SQLException;
    Qna getQna(int qIdx, String rId, int uIdx) throws SQLException;
    List<Qna> getQnaList() throws SQLException;
    void updateQna(Qna qna) throws SQLException;
    void deleteQna(int qIdx, String rId, int uIdx) throws SQLException;
}
