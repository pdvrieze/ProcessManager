DROP TABLE `users`;
CREATE TABLE `users` (
  `user` varchar(30) NOT NULL,
  `fullname` varchar(80),
  `password` varchar(40) NOT NULL,
  PRIMARY KEY (`user`) USING HASH
) ENGINE=InnoDB DEFAULT CHARSET=utf8;