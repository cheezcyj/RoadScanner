INSERT INTO MEMBER (no, id, password, email, grade)
VALUES (1, 'admin', 'test-only-hash', 'admin@example.test', 2);

INSERT INTO MEMBER (no, id, password, email, grade)
VALUES (2, 'member01', 'test-only-hash', 'member@example.test', 1);

INSERT INTO MEMBER (no, id, password, email, grade)
VALUES (3, 'banned01', 'test-only-hash', 'banned@example.test', 3);

INSERT INTO QUESTION (no, category, id, title, content)
VALUES (229, 30, 'admin', 'answer fixture', 'question for answer DAO tests');

INSERT INTO RESULT_IMAGE (no, name, content, url)
VALUES (1, 'cross.png', '十자형 교차로', 'https://example.test/cross.png');
INSERT INTO RESULT_IMAGE (no, name, content, url)
VALUES (2, 'tee.png', 'T자형 교차로', 'https://example.test/tee.png');
INSERT INTO RESULT_IMAGE (no, name, content, url)
VALUES (3, 'fork.png', 'Y자형 교차로', 'https://example.test/fork.png');
