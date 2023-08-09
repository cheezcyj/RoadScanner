DROP INDEX PK_QUESTION;

/* 문의게시판 */
DROP TABLE QUESTION
    CASCADE CONSTRAINTS;

/* 문의게시판 */
CREATE TABLE QUESTION (
                          no NUMBER(10) DEFAULT question_seq.NEXTVAL NOT NULL, /* 게시글번호 */
                          category NUMBER(2) DEFAULT 30 NOT NULL, /* 게시글분류 */
                          views NUMBER(5) DEFAULT 0 NOT NULL, /* 게시글조회수 */
                          idx NUMBER(10), /* 이미지ID */
                          id VARCHAR2(30) NOT NULL, /* 게시글작성자 */
                          title VARCHAR2(45) NOT NULL, /* 게시글제목 */
                          content CLOB NOT NULL, /* 게시글내용 */
                          create_date DATE DEFAULT SYSDATE, /* 게시글작성일 */
                          update_date DATE DEFAULT SYSDATE /* 게시글수정일 */
);

COMMENT ON COLUMN QUESTION.no IS 'SEQUENCE';

COMMENT ON COLUMN QUESTION.category IS '공지(10), 답변완료(20), 답변대기(30)';

CREATE UNIQUE INDEX PK_QUESTION
    ON QUESTION (
                 no ASC
        );

ALTER TABLE QUESTION
    ADD
        CONSTRAINT PK_QUESTION
            PRIMARY KEY (
                         no
                );

ALTER TABLE QUESTION
    ADD
        CONSTRAINT FK_MEMBER_TO_QUESTION
            FOREIGN KEY (
                         id
                )
                REFERENCES MEMBER (
                                   id
                    );

COMMIT;