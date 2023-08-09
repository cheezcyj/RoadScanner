DROP TABLE IF EXISTS QUESTION;

CREATE TABLE QUESTION (
                          no INT AUTO_INCREMENT PRIMARY KEY,
                          category INT DEFAULT 30 NOT NULL,
                          views INT DEFAULT 0 NOT NULL,
                          idx INT,
                          id VARCHAR(30) NOT NULL,
                          title VARCHAR(45) NOT NULL,
                          content CLOB NOT NULL,
                          create_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
                          update_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP()
);

DROP TABLE IF EXISTS MEMBER;

CREATE TABLE MEMBER (
                        id VARCHAR(30) NOT NULL PRIMARY KEY,
                        no INT AUTO_INCREMENT,
                        password VARCHAR(50) NOT NULL,
                        email VARCHAR(320) NOT NULL,
                        grade INT DEFAULT 1 NOT NULL,
                        reg_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP() NOT NULL
);

ALTER TABLE QUESTION ADD FOREIGN KEY (id) REFERENCES MEMBER(id);