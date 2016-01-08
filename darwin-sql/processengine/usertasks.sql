DROP TABLE `nodedata`;
DROP TABLE `usertasks`;

CREATE TABLE `usertasks` (
  `taskhandle` BIGINT NOT NULL AUTO_INCREMENT,
  `remotehandle` BIGINT NOT NULL,
  PRIMARY KEY ( `taskhandle` )
) ENGINE=InnoDB CHARSET=utf8;

CREATE TABLE `nodedata` (
  `name` VARCHAR(30) NOT NULL,
  `taskhandle` BIGINT NOT NULL,
  `data` TEXT,
  PRIMARY KEY ( `name`, `taskhandle` ),
  FOREIGN KEY ( `taskhandle` ) REFERENCES `usertasks` ( `taskhandle` )
) ENGINE=InnoDB CHARSET=utf8;
