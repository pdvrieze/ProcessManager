DROP TABLE IF EXISTS `processmodels`;
DROP TABLE IF EXISTS `pmusers`;
DROP TABLE IF EXISTS `pmroles`;

CREATE TABLE `processmodels` (
  `pmhandle` BIGINT NOT NULL AUTO_INCREMENT,
  `owner` varchar(30) NOT NULL,
  `model` MEDIUMTEXT,
  INDEX ( `owner` ),
  PRIMARY KEY ( `pmhandle` )
) ENGINE=InnoDB CHARSET=utf8;

CREATE TABLE `pmusers` (
  `pmhandle` BIGINT NOT NULL,
  `user` varchar(30) NOT NULL,
  PRIMARY KEY ( `pmhandle`, `user` ),
  FOREIGN KEY ( `pmhandle` ) REFERENCES `processmodels` ( `pmhandle` )
) ENGINE=InnoDB CHARSET=utf8;


CREATE TABLE `pmroles` (
  `pmhandle` BIGINT NOT NULL,
  `role` varchar(30) NOT NULL,
  PRIMARY KEY ( `pmhandle`, `role` ),
  FOREIGN KEY ( `pmhandle` ) REFERENCES `processmodels` ( `pmhandle` )
) ENGINE=InnoDB CHARSET=utf8;