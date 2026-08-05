-- Apply once to an existing Oracle schema before deploying this version.
ALTER TABLE MEMBER ADD (credential_version NUMBER(10) DEFAULT 0 NOT NULL);

-- Grade 4 is reserved for retired accounts. Existing authored/uploaded rows
-- continue to reference the account row, while login and recovery reject it.
