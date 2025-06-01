-- 테스트 데이터베이스 초기화 스크립트
-- 실행: mysql -u root -p1234 < reset-test-database.sql

DROP DATABASE IF EXISTS unibook_test_db;
CREATE DATABASE unibook_test_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE unibook_test_db;

-- 메인 애플리케이션을 test 프로파일로 한 번 실행해서 스키마를 생성하세요:
-- java -jar unibook-0.0.1-SNAPSHOT.jar --spring.profiles.active=test --spring.jpa.hibernate.ddl-auto=create

-- 또는 IntelliJ에서 Run Configuration에 VM options 추가:
-- -Dspring.profiles.active=test -Dspring.jpa.hibernate.ddl-auto=create