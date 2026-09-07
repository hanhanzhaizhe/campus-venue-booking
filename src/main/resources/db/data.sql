-- 种子数据（可重复执行）
-- password_hash 为 PENDING_BCRYPT 时，启动后由 SeedPasswordInitializer 写成 123456 的 BCrypt
USE venue_booking;

INSERT INTO `user` (`username`, `password_hash`, `role`, `status`)
VALUES
  ('admin',     'PENDING_BCRYPT', 'ADMIN',   'ACTIVE'),
  ('teacher01', 'PENDING_BCRYPT', 'TEACHER', 'ACTIVE'),
  ('stu01',     'PENDING_BCRYPT', 'STUDENT', 'ACTIVE')
ON DUPLICATE KEY UPDATE
  `role` = VALUES(`role`),
  `status` = VALUES(`status`);

INSERT INTO `venue` (`name`, `type`, `campus`, `building`, `capacity`, `open_start`, `open_end`, `status`)
SELECT '教学楼A-301', 'MEETING_ROOM', '主校区', '教学楼A', 20, '08:00:00', '22:00:00', 'ENABLED'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `venue` WHERE `name` = '教学楼A-301');
