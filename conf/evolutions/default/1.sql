-- Team schema

-- !Ups
CREATE TABLE Team (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    name varchar(255) NOT NULL,
    description varchar(1000) NOT NULL,
    address varchar(4000) NOT NULL,
    PRIMARY KEY (id)
);

-- !Downs
DROP TABLE Team;