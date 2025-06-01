-- 1. 기존 외래키 제약 제거
ALTER TABLE chat_rooms DROP FOREIGN KEY FK5a17nbflkx8cdvinwg1ipgxfx;

-- 2. post_id 컬럼을 nullable로 변경 (이미 nullable일 수도 있음)
ALTER TABLE chat_rooms MODIFY COLUMN post_id BIGINT NULL;

-- 3. 새로운 외래키 제약 추가 (ON DELETE SET NULL 옵션 포함)
ALTER TABLE chat_rooms 
ADD CONSTRAINT FK_chat_rooms_post_id 
FOREIGN KEY (post_id) REFERENCES posts(post_id) 
ON DELETE SET NULL;

-- 4. 변경사항 확인
SHOW CREATE TABLE chat_rooms;