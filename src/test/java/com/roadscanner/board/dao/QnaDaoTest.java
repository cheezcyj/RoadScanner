package com.roadscanner.board.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import oracle.jdbc.pool.OracleDataSource;

import com.roadscanner.board.domain.Qna;

public class QnaDaoTest {

    private static SqlSessionFactory sqlSessionFactory;
    private static DataSource dataSource;
    private QnaDao qnaDao;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Oracle 데이터베이스 설정
        OracleDataSource oracleDataSource = new OracleDataSource();
        oracleDataSource.setURL("jdbc:oracle:thin:@192.168.0.123:1521:XE");
        oracleDataSource.setUser("F1");
        oracleDataSource.setPassword("4444");
        dataSource = oracleDataSource;

        // MyBatis 설정
        Properties properties = new Properties();
        properties.setProperty("jdbc.driver", "oracle.jdbc.driver.OracleDriver");
        properties.setProperty("jdbc.url", "jdbc:oracle:thin:@192.168.0.123:1521:XE");
        properties.setProperty("jdbc.username", "F1");
        properties.setProperty("jdbc.password", "4444");

        // MyBatis Configuration 가져오기
        String resource = "mybatis-config.xml";
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream, properties);
        }

        // Oracle 스키마 및 테이블 생성을 위해 SQL 스크립트 실행
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            InputStream schemaInputStream = QnaDaoTest.class.getResourceAsStream("/schema.sql");
            sqlSession.getConnection().prepareStatement(readFileAsString(schemaInputStream)).execute();
            sqlSession.commit();
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (dataSource != null) {
            try (Connection connection = dataSource.getConnection()) {
                connection.close(); // 데이터베이스 연결 닫기
            }
        }
    }

    @Before
    public void setUp() {
        try (Connection connection = dataSource.getConnection()) {
            // 테스트 중에 발생한 변경 사항을 롤백
            connection.rollback();
            qnaDao = new QnaDao(connection);
        } catch (SQLException e) {
            e.printStackTrace();
            fail("QnaDao 초기화 실패: " + e.getMessage());
        }
    }

    @Test
    public void testCreateQna() throws SQLException {
        Qna newQna = new Qna();
        newQna.setrId("user123");
        newQna.setuIdx(1);
        newQna.setqDiv(1);
        newQna.setqReadCnt(0);
        newQna.setqTitle("새로운 게시물 제목");
        newQna.setqContents("새로운 게시물 내용");

        qnaDao.createQna(newQna);

        assertNotNull(newQna.getqIdx());
    }

    @Test
    public void testGetQna() throws SQLException {
        int qIdx = 1;
        String rId = "user123";
        int uIdx = 1;

        Qna retrievedQna = qnaDao.getQna(qIdx, rId, uIdx);

        assertNotNull(retrievedQna);
        assertEquals("새로운 게시물 제목", retrievedQna.getqTitle());
        assertEquals("새로운 게시물 내용", retrievedQna.getqContents());
    }

    @Test
    public void testGetQnaList() throws SQLException {
        List<Qna> qnaList = qnaDao.getQnaList();

        assertEquals(1, qnaList.size());
        assertEquals("새로운 게시물 제목", qnaList.get(0).getqTitle());
    }

    @Test
    public void testUpdateQna() throws SQLException {
        int qIdx = 1;
        String rId = "user123";
        int uIdx = 1;

        Qna retrievedQna = qnaDao.getQna(qIdx, rId, uIdx);
        assertNotNull(retrievedQna);

        retrievedQna.setqTitle("수정된 게시물 제목");
        retrievedQna.setqContents("수정된 게시물 내용");
        qnaDao.updateQna(retrievedQna);

        Qna updatedQna = qnaDao.getQna(qIdx, rId, uIdx);
        assertNotNull(updatedQna);
        assertEquals("수정된 게시물 제목", updatedQna.getqTitle());
        assertEquals("수정된 게시물 내용", updatedQna.getqContents());
    }

    @Test
    public void testDeleteQna() throws SQLException {
        int qIdx = 1;
        String rId = "user123";
        int uIdx = 1;

        Qna retrievedQna = qnaDao.getQna(qIdx, rId, uIdx);
        assertNotNull(retrievedQna);

        qnaDao.deleteQna(qIdx, rId, uIdx);

        retrievedQna = qnaDao.getQna(qIdx, rId, uIdx);
        assertNull(retrievedQna);
    }

    private static String readFileAsString(InputStream inputStream) {
        try (java.util.Scanner scanner = new java.util.Scanner(inputStream).useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        }
    }
}
