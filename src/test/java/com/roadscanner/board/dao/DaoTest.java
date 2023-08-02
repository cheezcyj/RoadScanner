package com.roadscanner.board.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.roadscanner.board.dao.QnaDao;
import com.roadscanner.board.domain.Qna;
import com.roadscanner.board.util.MyBatisUtil;

public class DaoTest {

    public static void main(String[] args) {
        try {
            // MyBatisUtil로부터 Connection을 가져옵니다.
            Connection connection = MyBatisUtil.getConnection();

            // QnaDao 생성 및 초기화
            QnaDao qnaDao = new QnaDao(connection);

            // 새로운 Qna 객체 생성
            Qna newQna = new Qna();
            newQna.setrId("user123");
            newQna.setuIdx(1);
            newQna.setqDiv(1);
            newQna.setqReadCnt(0);
            newQna.setqTitle("새로운 게시물 제목");
            newQna.setqContents("새로운 게시물 내용");

            // 게시물 생성
            qnaDao.createQna(newQna);
            System.out.println("새로운 게시물이 생성되었습니다.");

            // 생성된 게시물 조회
            int qIdx = newQna.getqIdx();
            Qna retrievedQna = qnaDao.getQna(qIdx, newQna.getrId(), newQna.getuIdx());
            if (retrievedQna != null) {
                System.out.println("조회된 게시물 제목: " + retrievedQna.getqTitle());
            } else {
                System.out.println("게시물을 찾을 수 없습니다.");
            }

            // 게시물 목록 조회
            List<Qna> qnaList = qnaDao.getQnaList();
            System.out.println("전체 게시물 목록:");
            for (Qna qna : qnaList) {
                System.out.println("게시물 제목: " + qna.getqTitle());
            }

            // 게시물 수정
            newQna.setqTitle("수정된 게시물 제목");
            newQna.setqContents("수정된 게시물 내용");
            qnaDao.updateQna(newQna);
            System.out.println("게시물이 수정되었습니다.");

            // 수정된 게시물 조회
            retrievedQna = qnaDao.getQna(qIdx, newQna.getrId(), newQna.getuIdx());
            if (retrievedQna != null) {
                System.out.println("수정된 게시물 제목: " + retrievedQna.getqTitle());
            } else {
                System.out.println("수정된 게시물을 찾을 수 없습니다.");
            }

            // 게시물 삭제
            qnaDao.deleteQna(qIdx, newQna.getrId(), newQna.getuIdx());
            System.out.println("게시물이 삭제되었습니다.");

            // 삭제된 게시물 조회
            retrievedQna = qnaDao.getQna(qIdx, newQna.getrId(), newQna.getuIdx());
            if (retrievedQna != null) {
                System.out.println("삭제된 게시물 제목: " + retrievedQna.getqTitle());
            } else {
                System.out.println("삭제된 게시물을 찾을 수 없습니다.");
            }

            // Connection 닫기
            connection.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
