DROP TABLE `boxes`;
CREATE TABLE `boxes` (
  `boxId` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) DEFAULT NULL,
  `owner` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`boxId`)
) ENGINE=InnoDB CHARSET=utf8
