DROP TABLE IF EXISTS `pnipredecessors`;
DROP TABLE IF EXISTS `nodedata`;
DROP TABLE IF EXISTS `instancedata`;
DROP TABLE IF EXISTS `processnodeinstances`;
DROP TABLE IF EXISTS `processinstances`;

CREATE TABLE `processinstances` (
  `pihandle` BIGINT NOT NULL AUTO_INCREMENT,
  `owner` varchar(30) NOT NULL,
  `name` varchar(50),
  `pmhandle` BIGINT NOT NULL,
  `state` varchar(15),
  `uuid` varchar(36), 
  INDEX ( `owner` ),
  PRIMARY KEY ( `pihandle` ),
  FOREIGN KEY ( `pmhandle` ) REFERENCES `processmodels` ( `pmhandle` )
) ENGINE=InnoDB CHARSET=utf8;

CREATE TABLE `processnodeinstances` (
  `pnihandle` BIGINT NOT NULL AUTO_INCREMENT,
  `pihandle` BIGINT NOT NULL,
  `nodeid` VARCHAR(30) NOT NULL,
  `state` VARCHAR(30) DEFAULT 'Sent',
  PRIMARY KEY ( `pnihandle` ),
  FOREIGN KEY ( `pihandle` ) REFERENCES `processinstances` ( `pihandle` )
) ENGINE=InnoDB CHARSET=utf8;

CREATE TABLE `pnipredecessors` (
  `pnihandle` BIGINT NOT NULL,
  `predecessor` BIGINT NOT NULL,
  PRIMARY KEY ( `pnihandle`, `predecessor` ),
  FOREIGN KEY ( `predecessor` ) REFERENCES `processnodeinstances` ( `pnihandle` ),
  FOREIGN KEY ( `pnihandle` ) REFERENCES `processnodeinstances` ( `pnihandle` )
) ENGINE=InnoDB CHARSET=utf8;

CREATE TABLE `instancedata` (
  `name` VARCHAR(30) NOT NULL,
  `pihandle` BIGINT NOT NULL,
  `data` TEXT NOT NULL,
  `isoutput` BOOLEAN NOT NULL,
  PRIMARY KEY ( `name`, `pihandle`, `isoutput` ),
  FOREIGN KEY ( `pihandle` ) REFERENCES `processinstances` ( `pihandle` )

) ENGINE=InnoDB CHARSET=utf8;

CREATE TABLE `nodedata` (
  `name` VARCHAR(30) NOT NULL,
  `pnihandle` BIGINT NOT NULL,
  `data` TEXT,
  PRIMARY KEY ( `name`, `pnihandle` ),
  FOREIGN KEY ( `pnihandle` ) REFERENCES `processnodeinstances` ( `pnihandle` )

) ENGINE=InnoDB CHARSET=utf8;

