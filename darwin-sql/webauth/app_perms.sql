DROP TABLE `app_perms`;

CREATE TABLE `app_perms` (
  `user` varchar(30) NOT NULL,
  `app` varchar(50) NOT NULL,
  PRIMARY KEY (`user`, `app`),
  FOREIGN KEY (`user`) references `users` (`user`)
) ENGINE=InnoDB CHARSET=utf8;