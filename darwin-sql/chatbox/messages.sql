DROP TABLE `messages`;
CREATE TABLE `messages` (
  `msgId` int(11) NOT NULL AUTO_INCREMENT,
  `boxId` int(11) NOT NULL,
  `msgIndex` int(11) NOT NULL,
  `message` mediumtext,
  `epoch` mediumtext NOT NULL,
  `sender` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`msgId`),
  UNIQUE KEY `boxIdMsgIdx` (`boxId`,`msgIndex`),
  FOREIGN KEY (`boxId`) REFERENCES `boxes` (`boxId`)
) ENGINE=InnoDB CHARSET=utf8