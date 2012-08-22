DROP TABLE `users`;
CREATE TABLE `users` (
  `user` varchar(30) NOT NULL,
  `password` varchar(40) NOT NULL,
  `user_group` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`user`) USING HASH
) ENGINE=InnoDB DEFAULT CHARSET=utf8;