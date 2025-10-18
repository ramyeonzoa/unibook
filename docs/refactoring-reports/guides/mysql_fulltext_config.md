# MySQL Full-text Search 설정 가이드

## Windows (my.ini) 설정
`C:\ProgramData\MySQL\MySQL Server 8.0\my.ini` 파일에 다음 내용 추가:

```ini
[mysqld]
# ngram 토큰 크기 설정 (한글 2글자 단위)
ngram_token_size=2

# Full-text 검색 최소 단어 길이 (기본값: 3)
ft_min_word_len=2
innodb_ft_min_token_size=2

# Full-text 검색 캐시 크기 (옵션)
innodb_ft_cache_size=80000000
innodb_ft_total_cache_size=1600000000
```

## 설정 적용 방법
1. MySQL 서비스 재시작
   - Windows: `services.msc` → MySQL80 재시작
   - 또는 명령 프롬프트(관리자): 
     ```
     net stop MySQL80
     net start MySQL80
     ```

2. 설정 확인
   ```sql
   SHOW VARIABLES LIKE '%ngram%';
   SHOW VARIABLES LIKE '%ft_%';
   ```

## 인덱스 생성
1. 프로젝트 루트에서 실행:
   ```
   mysql -u root -p1234 unibook_db < create_fulltext_indexes.sql
   ```

## 주의사항
- 인덱스 생성은 테이블 크기에 따라 시간이 걸릴 수 있음
- 운영 중인 서비스의 경우 트래픽이 적은 시간에 실행 권장