DROP TABLE `tokens`;

CREATE TABLE `tokens` (
  `tokenId` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`tokenId`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB CHARSET=utf8;