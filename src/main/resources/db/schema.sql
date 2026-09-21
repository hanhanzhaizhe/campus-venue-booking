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


-- ========== admin-audit 增量（feature/admin-audit）==========
CREATE TABLE IF NOT EXISTS `admin_audit_log` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT,
  `operator_id`    BIGINT        NOT NULL COMMENT '管理员 user.id',
  `action`         VARCHAR(32)   NOT NULL COMMENT '见 AdminAuditActions',
  `resource_type`  VARCHAR(16)   NOT NULL COMMENT 'RESERVATION / VENUE',
  `resource_id`    BIGINT        NOT NULL COMMENT '对应资源主键',
  `reservation_id` BIGINT        DEFAULT NULL COMMENT '预约维度查询冗余；场地类为空',
  `venue_id`       BIGINT        DEFAULT NULL COMMENT '场地 id 冗余，便于按场地查',
  `before_data`    VARCHAR(1000) DEFAULT NULL COMMENT '改前关键字段 JSON 摘要',
  `after_data`     VARCHAR(1000) DEFAULT NULL COMMENT '改后关键字段 JSON 摘要',
  `reason`         VARCHAR(200)  DEFAULT NULL COMMENT '可选原因；P0 接口无入参时恒为 null',
  `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_reservation_time` (`reservation_id`, `created_at`),
  KEY `idx_audit_operator_time` (`operator_id`, `created_at`),
  KEY `idx_audit_resource_time` (`resource_type`, `resource_id`, `created_at`)
) ENGINE=InnoDB COMMENT='管理端写操作审计（只追加）';
