DROP TABLE `tokens`;

CREATE TABLE `tokens` (
  `tokenid` int(11) NOT NULL AUTO_INCREMENT,
  `user` varchar(30) NOT NULL,
  `ip` varchar(24) NOT NULL,
  `keyid` int(11) NOT NULL,
  `token` varchar(45) NOT NULL,
  `epoch` int(11) NOT NULL,
  PRIMARY KEY (`tokenid`),
  FOREIGN KEY (`user`) REFERENCES `users` (`user`),
  FOREIGN KEY (`keyid`) REFERENCES `pubkeys` (`keyid`)
) ENGINE=InnoDB CHARSET=utf8;