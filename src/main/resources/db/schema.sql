-- 高校场地预约 P0 表结构（与冻结文档一致）
-- 执行：mysql -h127.0.0.1 -uroot -p --default-character-set=utf8mb4 < src/main/resources/db/schema.sql

CREATE DATABASE IF NOT EXISTS venue_booking
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE venue_booking;

CREATE TABLE IF NOT EXISTS `user` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `username`      VARCHAR(32)  NOT NULL COMMENT '登录名',
  `password_hash` VARCHAR(100) NOT NULL COMMENT 'BCrypt',
  `role`          VARCHAR(16)  NOT NULL COMMENT 'STUDENT / TEACHER / ADMIN',
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'P0 仅 ACTIVE',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB COMMENT='用户';

CREATE TABLE IF NOT EXISTS `venue` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `name`       VARCHAR(64)  NOT NULL,
  `type`       VARCHAR(32)  NOT NULL COMMENT 'MEETING_ROOM / SPORTS',
  `campus`     VARCHAR(32)  NOT NULL,
  `building`   VARCHAR(64)  DEFAULT NULL,
  `capacity`   INT          DEFAULT NULL COMMENT '仅展示',
  `open_start` TIME         NOT NULL COMMENT '每日开放开始',
  `open_end`   TIME         NOT NULL COMMENT '每日开放结束（不含）',
  `status`     VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED / DISABLED',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_venue_status_type` (`status`, `type`)
) ENGINE=InnoDB COMMENT='独占型场地';

CREATE TABLE IF NOT EXISTS `reservation` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `venue_id`   BIGINT       NOT NULL,
  `user_id`    BIGINT       NOT NULL,
  `start_time` DATETIME     NOT NULL COMMENT '含',
  `end_time`   DATETIME     NOT NULL COMMENT '不含',
  `status`     VARCHAR(16)  NOT NULL COMMENT 'CONFIRMED / CANCELLED',
  `purpose`    VARCHAR(100) DEFAULT NULL,
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_res_venue_time` (`venue_id`, `start_time`, `end_time`),
  KEY `idx_res_user_status` (`user_id`, `status`),
  CONSTRAINT `fk_res_venue` FOREIGN KEY (`venue_id`) REFERENCES `venue` (`id`),
  CONSTRAINT `fk_res_user`  FOREIGN KEY (`user_id`)  REFERENCES `user` (`id`)
) ENGINE=InnoDB COMMENT='预约';
