DROP TABLE `processmodels`;
DROP TABLE `pmusers`;
DROP TABLE `pmroles`;

CREATE TABLE `processmodels` (
  `pmhandle` INT NOT NULL AUTO_INCREMENT,
  `owner` varchar(30) NOT NULL,
  `model` MEDIUMBLOB,
  INDEX ( `owner` ),
  PRIMARY KEY ( `pmhandle` )
) ENGINE=InnoDB CHARSET=utf8;

CREATE TABLE `pmusers` (
  `pmhandle` INT NOT NULL,
  `user` varchar(30) NOT NULL,
  PRIMARY KEY ( `pmhandle`, `user` ),
  FOREIGN KEY ( `pmhandle` ) REFERENCES `processmodels` ( `pmhandle` )
) ENGINE=InnoDB CHARSET=utf8;


CREATE TABLE `pmroles` (
  `pmhandle` INT NOT NULL,
  `role` varchar(30) NOT NULL,
  PRIMARY KEY ( `pmhandle`, `role` ),
  FOREIGN KEY ( `pmhandle` ) REFERENCES `processmodels` ( `pmhandle` )
) ENGINE=InnoDB CHARSET=utf8;