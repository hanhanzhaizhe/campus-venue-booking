-- 第 4 步占用查询演示数据（可重复执行）
-- 2026-09-10 10:00-11:00 一条 CONFIRMED；同日 14:00-15:00 一条 CANCELLED（占用接口应忽略）
USE venue_booking;

INSERT INTO `reservation` (`venue_id`, `user_id`, `start_time`, `end_time`, `status`, `purpose`)
SELECT v.id, u.id, '2026-09-10 10:00:00', '2026-09-10 11:00:00', 'CONFIRMED', '占用查询演示'
FROM `venue` v
JOIN `user` u ON u.username = 'stu01'
WHERE v.name = '体育馆羽毛球场1'
  AND NOT EXISTS (
      SELECT 1 FROM `reservation` r
      WHERE r.venue_id = v.id
        AND r.start_time = '2026-09-10 10:00:00'
        AND r.end_time = '2026-09-10 11:00:00'
        AND r.status = 'CONFIRMED'
  )
LIMIT 1;

INSERT INTO `reservation` (`venue_id`, `user_id`, `start_time`, `end_time`, `status`, `purpose`)
SELECT v.id, u.id, '2026-09-10 14:00:00', '2026-09-10 15:00:00', 'CANCELLED', '已取消不应占用'
FROM `venue` v
JOIN `user` u ON u.username = 'stu01'
WHERE v.name = '体育馆羽毛球场1'
  AND NOT EXISTS (
      SELECT 1 FROM `reservation` r
      WHERE r.venue_id = v.id
        AND r.start_time = '2026-09-10 14:00:00'
        AND r.end_time = '2026-09-10 15:00:00'
        AND r.status = 'CANCELLED'
  )
LIMIT 1;
