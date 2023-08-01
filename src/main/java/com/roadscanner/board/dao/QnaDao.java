package com.roadscanner.board.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.apache.ibatis.session.SqlSession;

import com.roadscanner.board.domain.Qna;
import com.roadscanner.board.util.MyBatisUtil;

public class QnaDao {
    private Connection connection;

    public QnaDao(Connection connection) {
        this.connection = connection;
    }

    // 게시물 생성
    public void createQna(Qna qna) throws SQLException {
        try (SqlSession sqlSession = MyBatisUtil.getSqlSessionFactory().openSession()) {
            sqlSession.insert("com.roadscanner.board.dao.QnaDao.createQna", qna);
            sqlSession.commit();
        }
    }

    // 게시물 조회
    public Qna getQna(int qIdx, String rId, int uIdx) throws SQLException {
        try (SqlSession sqlSession = MyBatisUtil.getSqlSessionFactory().openSession()) {
            QnaDao mapper = sqlSession.getMapper(QnaDao.class);
            return mapper.getQna(qIdx, rId, uIdx);
        }
    }

    // 게시물 목록 조회
    public List<Qna> getQnaList() throws SQLException {
        try (SqlSession sqlSession = MyBatisUtil.getSqlSessionFactory().openSession()) {
            QnaDao mapper = sqlSession.getMapper(QnaDao.class);
            return mapper.getQnaList();
        }
    }

    // 게시물 수정
    public void updateQna(Qna qna) throws SQLException {
        try (SqlSession sqlSession = MyBatisUtil.getSqlSessionFactory().openSession()) {
            sqlSession.update("com.roadscanner.board.dao.QnaDao.updateQna", qna);
            sqlSession.commit();
        }
    }

    // 게시물 삭제
    public void deleteQna(int qIdx, String rId, int uIdx) throws SQLException {
        try (SqlSession sqlSession = MyBatisUtil.getSqlSessionFactory().openSession()) {
            sqlSession.delete("com.roadscanner.board.dao.QnaDao.deleteQna", new Qna());
            sqlSession.commit();
        }
    }
}
