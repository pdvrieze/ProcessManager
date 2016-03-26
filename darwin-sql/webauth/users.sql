DROP TABLE `users`;
CREATE TABLE `users` (
  `user` VARCHAR(30) NOT NULL,
  `fullname` VARCHAR(80),
  `alias` VARCHAR(80),
  `password` VARCHAR(40) NOT NULL,
  `resettoken` VARCHAR(20),
  `resettime` DATETIME,
  PRIMARY KEY (`user`) USING HASH
) ENGINE=InnoDB DEFAULT CHARSET=utf8;