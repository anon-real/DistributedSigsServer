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
    pk VARCHAR(1000) NOT NULL,
    team_id BIGINT(20) NOT NULL,
    FOREIGN KEY (team_id) REFERENCES Team(id) ON DELETE CASCADE,
    PRIMARY KEY (pk, team_id)
);

CREATE TABLE Request (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    title VARCHAR(1000) NOT NULL,
    status VARCHAR(100) NOT NULL,
    amount BIGINT NOT NULL,
    description VARCHAR(2000) NOT NULL,
    address VARCHAR(4000) NOT NULL,
    team_id BIGINT(20) NOT NULL,
    time DATETIME NOT NULL DEFAULT(CURRENT_TIMESTAMP()),
    FOREIGN KEY (team_id) REFERENCES Team(id) ON DELETE CASCADE,
    PRIMARY KEY (id)
);

CREATE TABLE Commitment (
    pk VARCHAR(1000) NOT NULL,
    a VARCHAR(1000) NOT NULL,
    request_id BIGINT(20) NOT NULL,
    time DATETIME NOT NULL DEFAULT(CURRENT_TIMESTAMP()),
    FOREIGN KEY (request_id) REFERENCES Request(id) ON DELETE CASCADE,
    FOREIGN KEY (pk) REFERENCES Member(pk) ON DELETE CASCADE,
    PRIMARY KEY (a)
);

CREATE TABLE Transaction (
    request_id BIGINT(20) NOT NULL,
    is_partial BIT NOT NULL DEFAULT(1),
    tx_bytes BLOB NOT NULL,
    is_valid BIT NOT NULL DEFAULT(0),
    is_confirmed BIT NOT NULL DEFAULT(0),
    FOREIGN KEY (request_id) REFERENCES Request(id) ON DELETE CASCADE,
    PRIMARY KEY (request_id, is_partial)
);

-- !Downs
DROP TABLE Commitment
DROP TABLE Request;
DROP TABLE Member;
DROP TABLE Team;
