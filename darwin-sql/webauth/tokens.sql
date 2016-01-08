DROP TABLE `tokens`;

CREATE TABLE `tokens` (
  `tokenid` int(11) NOT NULL AUTO_INCREMENT,
  `user` varchar(30) NOT NULL,
  `ip` varchar(24) NOT NULL,
  `keyid` int(11),
  `token` varchar(45) NOT NULL,
  `epoch` bigint NOT NULL,
  PRIMARY KEY (`tokenid`),
  FOREIGN KEY (`user`) REFERENCES `users` (`user`),
  FOREIGN KEY (`keyid`) REFERENCES `pubkeys` (`keyid`)
) ENGINE=InnoDB CHARSET=utf8;

GRANT DELETE ON `tokens` TO `webauth`@`localhost`;
