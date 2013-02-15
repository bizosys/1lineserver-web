drop schema if exists `smsdb`;
create schema `smsdb`;
use smsdb;

--
-- Table structure for table `user_login`
--

DROP TABLE IF EXISTS `user_login`;
CREATE TABLE  `user_login` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `active` char(1) NOT NULL DEFAULT 'Y',
  `loginid` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `profile` mediumtext NOT NULL,
  `touchTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_loginid` (`loginid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `app_config`;
CREATE TABLE  `app_config` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `configtype` char(1) NOT NULL,
  `title` varchar(64) NOT NULL,
  `body` mediumtext,
  `status` char(1) NOT NULL,
  `touchTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `BYTYPE` (`configtype`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `failed_email`;
CREATE TABLE  `failed_email` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `mail_to` varchar(255) NOT NULL,
  `mail_cc` varchar(255) DEFAULT NULL,
  `msg_subject` varchar(255) DEFAULT NULL,
  `msg_body` varchar(255) DEFAULT NULL,
  `touchTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `attachments` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `Index_2` (`touchTime`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;