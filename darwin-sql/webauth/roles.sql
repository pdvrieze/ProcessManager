DROP TABLE `roles`;
CREATE TABLE `roles` (
  `role` varchar(30) NOT NULL,
  `description` varchar(120) DEFAULT NULL,
  PRIMARY KEY (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;