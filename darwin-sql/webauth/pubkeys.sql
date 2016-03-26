DROP TABLE `pubkeys`;

CREATE TABLE `pubkeys` (
  `keyid` int(11) NOT NULL AUTO_INCREMENT,
  `user` varchar(30) NOT NULL,
  `appname` VARCHAR(80),
  `pubkey` mediumtext NOT NULL,
  lastUse BIGINT(20),
  PRIMARY KEY (`keyid`),
  FOREIGN KEY (`user`) REFERENCES `users` (`user`)
) ENGINE=InnoDB CHARSET=utf8
