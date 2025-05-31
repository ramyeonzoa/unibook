-- MySQL 채팅 데이터 완전 삭제 SQL
-- ⚠️ 주의: 이 스크립트는 모든 채팅 관련 데이터를 삭제합니다!

-- 1. 채팅 관련 알림 삭제 (있다면)
DELETE FROM notifications
WHERE type IN ('NEW_MESSAGE', 'CHAT_MESSAGE') OR content LIKE '%채팅%' OR content LIKE '%메시지%';

-- 2. 채팅방 테이블 완전 삭제
DELETE FROM chat_room;

-- 3. AUTO_INCREMENT 값 리셋 (새로 시작할 때 ID를 1부터 시작)
ALTER TABLE chat_room AUTO_INCREMENT = 1;

-- 4. 확인용 쿼리들
SELECT COUNT(*) as remaining_chatrooms FROM chat_room;
SELECT COUNT(*) as chat_notifications FROM notification WHERE type IN ('NEW_MESSAGE', 'CHAT_MESSAGE');

-- 5. 전체 테이블 상태 확인
SHOW TABLE STATUS LIKE 'chat_room';

-- 사용법:
-- 1. MySQL에 접속: mysql -u root -p1234
-- 2. 데이터베이스 선택: USE unibook_db;
-- 3. 이 파일 실행: SOURCE /mnt/c/dev/unibook/clear-mysql-chat-data.sql;