-- ChatRoom 테이블의 외래키 제약조건 확인
SHOW CREATE TABLE chat_rooms;

-- 외래키 제약조건 이름 확인 (보통 FK로 시작)
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM 
    INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE 
    TABLE_SCHEMA = 'unibook_db' 
    AND TABLE_NAME = 'chat_rooms'
    AND REFERENCED_TABLE_NAME = 'posts';

-- 외래키 제약조건 제거 (위에서 확인한 제약조건 이름 사용)
-- 예시: ALTER TABLE chat_rooms DROP FOREIGN KEY FKxxxxx;
-- ALTER TABLE chat_rooms DROP FOREIGN KEY [제약조건_이름];

-- 일반적인 이름으로 시도
ALTER TABLE chat_rooms DROP FOREIGN KEY fk_chatroom_post;

-- 또는 JPA가 자동 생성한 이름으로 시도
-- ALTER TABLE chat_rooms DROP FOREIGN KEY FK + 해시값;