DROP TABLE `nodedata`;
DROP TABLE `usertasks`;

CREATE TABLE `usertasks` (
  `taskhandle` BIGINT NOT NULL AUTO_INCREMENT,
  `remotehandle` varchar(30) NOT NULL,
  PRIMARY KEY ( `taskhandle` ),
  FOREIGN KEY ( `remotehandle` )
) ENGINE=InnoDB CHARSET=utf8;

CREATE TABLE `nodedata` (
  `name` VARCHAR(30) NOT NULL,
  `taskhandle` BIGINT NOT NULL,
  `data` TEXT NOT NULL,
  PRIMARY KEY ( `name`, `taskhandle` ),
  FOREIGN KEY ( `taskhandle` ) REFERENCES `processnodeinstances` ( `taskhandle` )
) ENGINE=InnoDB CHARSET=utf8;
