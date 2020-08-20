-- Team schema

-- !Ups
CREATE TABLE Team (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    address VARCHAR(4000) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE Member (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    nick_name VARCHAR(100) NOT NULL,
    public_key VARCHAR(1000) NOT NULL,
    team_id BIGINT(20) NOT NULL,
    FOREIGN KEY (team_id) REFERENCES Team(id) ON DELETE CASCADE,
    UNIQUE (team_id, public_key),
    PRIMARY KEY (id)
);

CREATE TABLE Request (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    title VARCHAR(1000) NOT NULL,
    status VARCHAR(100) NOT NULL,
    confirmed_tx_id VARCHAR(300) DEFAULT NULL,
    amount DOUBLE NOT NULL,
    description VARCHAR(2000) NOT NULL,
    address VARCHAR(4000) NOT NULL,
    team_id BIGINT(20) NOT NULL,
    time DATETIME NOT NULL DEFAULT(CURRENT_TIMESTAMP()),
    FOREIGN KEY (team_id) REFERENCES Team(id) ON DELETE CASCADE,
    PRIMARY KEY (id)
);

CREATE TABLE Commitment (
    member_id BIGINT(20) NOT NULL,
    a VARCHAR(1000) NOT NULL,
    request_id BIGINT(20) NOT NULL,
    time DATETIME NOT NULL DEFAULT(CURRENT_TIMESTAMP()),
    FOREIGN KEY (request_id) REFERENCES Request(id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES Member(id) ON DELETE CASCADE,
    PRIMARY KEY (request_id, member_id)
);

CREATE TABLE Proof (
    member_id BIGINT(20) NOT NULL,
    request_id BIGINT(20) NOT NULL,
    proof VARCHAR(10000) NOT NULL,
    contains_simulation BIT NOT NULL DEFAULT(0),
    FOREIGN KEY (request_id) REFERENCES Request(id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES Member(id) ON DELETE CASCADE,
    PRIMARY KEY (request_id, member_id)
);

CREATE TABLE UnsignedTransaction (
    request_id BIGINT(20) NOT NULL,
    tx_bytes BLOB NOT NULL,
    FOREIGN KEY (request_id) REFERENCES Request(id) ON DELETE CASCADE,
    PRIMARY KEY (request_id)
);

-- !Downs
DROP TABLE UnsignedTransaction ;
DROP TABLE COMMITMENT;
DROP TABLE REQUEST;
DROP TABLE MEMEBER;
DROP TABLE TEAM;
