package com.roadscanner.board.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.roadscanner.board.dao.QnaDao;
import com.roadscanner.board.domain.Qna;

public class QnaServiceImpl implements QnaService {
    private QnaDao qnaDao;

    public QnaServiceImpl(Connection connection) {
        qnaDao = new QnaDao(connection);
    }

    @Override
    public Qna getQna(int qIdx, String rId, int uIdx) throws SQLException {
        return qnaDao.getQna(qIdx, rId, uIdx);
    }

    @Override
    public List<Qna> getQnaList() throws SQLException {
        return qnaDao.getQnaList();
    }

    @Override
    public void updateQna(Qna qna) throws SQLException {
        qnaDao.updateQna(qna);
    }

    @Override
    public void deleteQna(int qIdx, String rId, int uIdx) throws SQLException {
        qnaDao.deleteQna(qIdx, rId, uIdx);
    }

    @Override
    public void createQna(Qna qna) throws SQLException {
        qnaDao.createQna(qna);
    }
}

