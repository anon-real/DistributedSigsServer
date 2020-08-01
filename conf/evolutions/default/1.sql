-- Team schema

-- !Ups
CREATE TABLE Team (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    name varchar(255) NOT NULL,
    description varchar(1000) NOT NULL,
    address varchar(4000) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE Request (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    title varchar(1000) NOT NULL,
    status varchar(100) NOT NULL,
    amount bigint NOT NULL,
    description varchar(2000) NOT NULL,
    address varchar(4000) NOT NULL,
    team_id integer not null,
    foreign key (team_id) references Team(id) on delete cascade,
    PRIMARY KEY (id)
);

-- !Downs
DROP TABLE Request;
DROP TABLE Team;
