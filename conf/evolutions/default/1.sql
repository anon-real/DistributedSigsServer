-- Team schema

-- !Ups
CREATE TABLE Team (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    name varchar(255) NOT NULL,
    description varchar(1000) NOT NULL,
    address varchar(4000) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE Member (
    pk varchar(1000) NOT NULL,
    team_id bigint(20) NOT NULL,
    foreign key (team_id) references Team(id) on delete cascade,
    PRIMARY KEY (pk, team_id)
);

CREATE TABLE Request (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    title varchar(1000) NOT NULL,
    status varchar(100) NOT NULL,
    amount bigint NOT NULL,
    description varchar(2000) NOT NULL,
    address varchar(4000) NOT NULL,
    team_id bigint(20) NOT NULL,
    time DATETIME NOT NULL DEFAULT(CURRENT_TIMESTAMP()),
    foreign key (team_id) references Team(id) on delete cascade,
    PRIMARY KEY (id)
);

CREATE TABLE Commitment (
    pk varchar(1000) NOT NULL,
    a varchar(1000) NOT NULL,
    request_id bigint(20) NOT NULL,
    time DATETIME NOT NULL DEFAULT(CURRENT_TIMESTAMP()),
    foreign key (request_id) references Request(id) on delete cascade,
    foreign key (pk) references Member(pk) on delete cascade,
    PRIMARY KEY (a)
);

-- !Downs
DROP TABLE Commitment
DROP TABLE Request;
DROP TABLE Member;
DROP TABLE Team;
