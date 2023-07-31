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
        } catch (SQLException e) {
            // 예외 처리 로직
            e.printStackTrace();
            throw e;
        }
    }

    // 게시물 조회
    public Qna getQna(int qIdx, String rId, int uIdx) throws SQLException {
        String sql = "SELECT * FROM F1.QNA WHERE Q_IDX = ? AND R_ID = ? AND U_IDX = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, qIdx);
            pstmt.setString(2, rId);
            pstmt.setInt(3, uIdx);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // ResultSet에서 결과를 읽어와서 Qna 객체를 생성하고 반환
                    Qna qna = new Qna();
                    qna.setqIdx(rs.getInt("Q_IDX"));
                    qna.setrId(rs.getString("R_ID"));
                    qna.setuIdx(rs.getInt("U_IDX"));
                    qna.setqDiv(rs.getInt("Q_DIV"));
                    qna.setqReadCnt(rs.getInt("Q_READ_CNT"));
                    qna.setqTitle(rs.getString("Q_TITLE"));
                    qna.setqContents(rs.getString("Q_CONTENTS"));
                    qna.setqRegDate(rs.getDate("Q_REG_DATE"));
                    qna.setqModDate(rs.getDate("Q_MOD_DATE"));
                    qna.setaId(rs.getString("A_ID"));
                    qna.setaContents(rs.getString("A_CONTENTS"));
                    qna.setaRegDate(rs.getDate("A_REG_DATE"));
                    qna.setaModDate(rs.getDate("A_MOD_DATE"));
                    return qna;
                } else {
                    return null; // 해당하는 게시물이 없는 경우 null 반환
                }
            }
        }catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    // 게시물 목록 조회
    public List<Qna> getQnaList() throws SQLException {
        List<Qna> qnaList = new ArrayList<>();
        String sql = "SELECT * FROM F1.QNA";
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Qna qna = new Qna();
                qna.setqIdx(rs.getInt("Q_IDX"));
                qna.setrId(rs.getString("R_ID"));
                qna.setuIdx(rs.getInt("U_IDX"));
                qna.setqDiv(rs.getInt("Q_DIV"));
                qna.setqReadCnt(rs.getInt("Q_READ_CNT"));
                qna.setqTitle(rs.getString("Q_TITLE"));
                qna.setqContents(rs.getString("Q_CONTENTS"));
                qna.setqRegDate(rs.getDate("Q_REG_DATE"));
                qna.setqModDate(rs.getDate("Q_MOD_DATE"));
                qna.setaId(rs.getString("A_ID"));
                qna.setaContents(rs.getString("A_CONTENTS"));
                qna.setaRegDate(rs.getDate("A_REG_DATE"));
                qna.setaModDate(rs.getDate("A_MOD_DATE"));
                qnaList.add(qna);
            }
        }catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        return qnaList;
    }

    // 게시물 수정
    public void updateQna(Qna qna) throws SQLException {
        String sql = "UPDATE F1.QNA SET Q_TITLE = ?, Q_CONTENTS = ?, Q_MOD_DATE = SYSDATE WHERE Q_IDX = ? AND R_ID = ? AND U_IDX = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, qna.getqTitle());
            pstmt.setString(2, qna.getqContents());
            pstmt.setInt(3, qna.getqIdx());
            pstmt.setString(4, qna.getrId());
            pstmt.setInt(5, qna.getuIdx());
            pstmt.executeUpdate();
        }catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    // 게시물 삭제
    public void deleteQna(int qIdx, String rId, int uIdx) throws SQLException {
        String sql = "DELETE FROM F1.QNA WHERE Q_IDX = ? AND R_ID = ? AND U_IDX = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, qIdx);
            pstmt.setString(2, rId);
            pstmt.setInt(3, uIdx);
            pstmt.executeUpdate();
        }catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }
}
