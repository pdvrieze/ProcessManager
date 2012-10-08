DROP TABLE `processinstances`;
DROP TABLE `processnodeinstances`;
DROP TABLE `nodedata`;

CREATE TABLE `processinstances` (
  `pihandle` BIGINT NOT NULL AUTO_INCREMENT,
  `owner` varchar(30) NOT NULL,
  `name` varchar(50),
  `pmhandle` BIGINT NOT NULL,
  INDEX ( `owner` ),
  PRIMARY KEY ( `pihandle` ),
  FOREIGN KEY ( `pmhandle` ) REFERENCES `processmodels` ( `pmhandle` )
) ENGINE=InnoDB CHARSET=utf8;

CREATE TABLE `processnodeinstances` (
  `pnihandle` BIGINT NOT NULL AUTO_INCREMENT,
  `pihandle` BIGINT NOT NULL,
  `nodeid` VARCHAR(30) NOT NULL,
  `predecessor` BIGINT,
  PRIMARY KEY ( `pnihandle` ),
  FOREIGN KEY ( `predecessor` ) REFERENCES `processnodeinstances` ( `pnihandle` )
) ENGINE=InnoDB CHARSET=utf8;

CREATE TABLE `nodedata` (
  `name` VARCHAR(30) NOT NULL,
  `pnihandle` BIGINT NOT NULL,
  `data` TEXT NOT NULL,
  PRIMARY KEY ( `name`, `pnihandle` ),
  FOREIGN KEY ( `pnihandle` ) REFERENCES `processnodeinstances` ( `pnihandle` )
  
) ENGINE=InnoDB CHARSET=utf8;
