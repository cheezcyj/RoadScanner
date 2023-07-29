package com.roadscanner.board.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.roadscanner.board.domain.Qna;

public class QnaDao {
    private Connection connection;

    public QnaDao(Connection connection) {
        this.connection = connection;
    }

    // 게시물 생성
    public void createQna(Qna qna) throws SQLException {
        String sql = "INSERT INTO F1.QNA (Q_IDX, R_ID, U_IDX, Q_DIV, Q_READ_CNT, Q_TITLE, Q_CONTENTS, Q_REG_DATE) "
                + "VALUES (F1.Q_IDX_SEQ.NEXTVAL, ?, ?, ?, ?, ?, ?, SYSDATE)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, qna.getrId());
            pstmt.setInt(2, qna.getuIdx());
            pstmt.setInt(3, qna.getqDiv());
            pstmt.setInt(4, qna.getqReadCnt());
            pstmt.setString(5, qna.getqTitle());
            pstmt.setString(6, qna.getqContents());
            pstmt.executeUpdate();
        }
    }

    // 게시물 조회
    public Qna getQna(int qIdx, String rId, int uIdx) throws SQLException {
        // TODO: Implement getQna method
        return null;
    }

    // 게시물 목록 조회
    public List<Qna> getQnaList() throws SQLException {
        // TODO: Implement getQnaList method
        return null;
    }

    // 게시물 수정
    public void updateQna(Qna qna) throws SQLException {
        // TODO: Implement updateQna method
    }

    // 게시물 삭제
    public void deleteQna(int qIdx, String rId, int uIdx) throws SQLException {
        // TODO: Implement deleteQna method
    }
}
