DROP TABLE `users`;
CREATE TABLE `users` (
  `user` varchar(30) NOT NULL,
  `fullname` varchar(80),
  `password` varchar(40) NOT NULL,
  `resettoken` VARCHAR(20),
  `resettime` DATETIME,
  PRIMARY KEY (`user`) USING HASH
) ENGINE=InnoDB DEFAULT CHARSET=utf8;