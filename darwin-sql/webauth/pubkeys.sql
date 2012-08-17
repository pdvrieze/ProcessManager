DROP TABLE `pubkeys`;

CREATE TABLE `pubkeys` (
  `keyid` int(11) NOT NULL AUTO_INCREMENT,
  `user` varchar(30) NOT NULL,
  `privkey` mediumtext NOT NULL,
  PRIMARY KEY (`keyid`),
  FOREIGN KEY (`user`) REFERENCES `users` (`user`)
) ENGINE=InnoDB CHARSET=utf8