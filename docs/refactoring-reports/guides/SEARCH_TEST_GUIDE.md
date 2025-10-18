# 검색 기능 테스트 가이드

## 1. MySQL 설정 및 인덱스 생성

### 1.1 MySQL 설정 (Windows)
`C:\ProgramData\MySQL\MySQL Server 8.0\my.ini` 파일에 추가:
```ini
[mysqld]
ngram_token_size=2
ft_min_word_len=2
innodb_ft_min_token_size=2
```

### 1.2 MySQL 재시작
```cmd
net stop MySQL80
net start MySQL80
```

### 1.3 Full-text 인덱스 생성
```bash
mysql -u root -p1234 unibook_db < create_fulltext_indexes.sql
```

## 2. 애플리케이션 실행

IntelliJ에서 실행 또는:
```bash
./gradlew bootRun
```

## 3. 검색 테스트

### 3.1 웹 브라우저에서 테스트
1. http://localhost:8080/posts 접속
2. 검색창에 키워드 입력:
   - "자료구조" - 단일 키워드 검색
   - "자료구조 교재" - AND 검색 (둘 다 포함)
   - "알고리즘" - 제목, 설명, 책 정보에서 검색

### 3.2 필터 조합 테스트
- 상품 타입 선택 + 검색
- 상태 선택 + 검색
- 검색어 없이 필터만 적용

### 3.3 검색 결과 확인
- 관련도순 정렬 확인
- 관련도가 같으면 최신순 정렬
- 페이징 동작 확인

## 4. 로그 확인

application.yml에서 로그 레벨 설정:
```yaml
logging:
  level:
    com.unibook.service.PostService: DEBUG
```

로그에서 확인할 내용:
- "Full-text 검색 실행" - 검색 쿼리 확인
- "필터링 조회" - 필터만 적용될 때
- "검색어가 너무 짧음" - 2글자 미만일 때

## 5. 주의사항

1. **인덱스 생성 확인**
   ```sql
   SHOW INDEX FROM post WHERE Key_name LIKE 'ft_%';
   ```

2. **ngram 설정 확인**
   ```sql
   SHOW VARIABLES LIKE 'ngram_token_size';
   ```

3. **테스트 데이터 준비**
   - 다양한 제목의 게시글
   - 설명이 있는 게시글
   - 책이 연결된 게시글

## 6. 문제 해결

### 검색이 안 될 때
1. Full-text 인덱스 생성 여부 확인
2. ngram_token_size 설정 확인
3. 검색어가 2글자 이상인지 확인

### 한글 검색이 안 될 때
1. MySQL 버전 확인 (5.7+ 필요)
2. ngram parser 설정 확인
3. 데이터베이스 character set 확인 (utf8mb4)

### 성능이 느릴 때
1. 인덱스 통계 업데이트
   ```sql
   ANALYZE TABLE post;
   ANALYZE TABLE post_description;
   ANALYZE TABLE book;
   ```

2. 쿼리 실행 계획 확인
   ```sql
   EXPLAIN SELECT ... (검색 쿼리)
   ```