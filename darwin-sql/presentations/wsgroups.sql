DROP TABLE members;
DROP TABLE wsgroups;
DROP TABLE slots;

CREATE TABLE `slots` (
  `slot` INT NOT NULL AUTO_INCREMENT,
  `description` VARCHAR(80),
  `date` DATE,
  PRIMARY KEY ( `slot` )
) ENGINE=InnoDB CHARSET=utf8;

CREATE TABLE `wsgroups` (
  `hgroup` INT NOT NULL AUTO_INCREMENT,
  `topic` MEDIUMTEXT,
  `slot` INT,
  PRIMARY KEY ( `hgroup` ),
  FOREIGN KEY ( `slot` ) REFERENCES `slots` (`slot`)
) ENGINE=InnoDB CHARSET=utf8;

CREATE TABLE `members` (
  `user` VARCHAR(30) NOT NULL,
  `hgroup` INT NOT NULL,
  PRIMARY KEY( `user` ),
  FOREIGN KEY( `hgroup` ) REFERENCES `wsgroups` ( `hgroup` )
) ENGINE=InnoDB CHARSET=utf8;
