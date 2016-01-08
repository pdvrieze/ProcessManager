DROP TABLE `user_roles`;
CREATE TABLE `user_roles` (
  `user` varchar(30) NOT NULL,
  `role` varchar(40) NOT NULL,
  PRIMARY KEY (`user`, `role`),
  FOREIGN KEY (`user`) REFERENCES `users` (`user`),
  FOREIGN KEY (`role`) REFERENCES `roles` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;