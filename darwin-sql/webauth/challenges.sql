DROP TABLE IF EXISTS `challenges`;

CREATE TABLE `challenges` (
  `keyid` int(11) NOT NULL DEFAULT '0',
  `challenge` varchar(100) DEFAULT NULL,
  `requestip` varchar(24) NOT NULL DEFAULT '',
  `epoch` int(11) DEFAULT NULL,
  PRIMARY KEY (`keyid`,`requestip`),
  FOREIGN KEY (`keyid`) REFERENCES `pubkeys` (`keyid`)
) ENGINE=InnoDB CHARSET=utf8;