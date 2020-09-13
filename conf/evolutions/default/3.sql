-- Update Team

-- !Ups
ALTER TABLE Commitment ALTER a VARCHAR(2000) NOT NULL;

-- !Downs
ALTER TABLE Commitment ALTER a VARCHAR(1000) NOT NULL;
