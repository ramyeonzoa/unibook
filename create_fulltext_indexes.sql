-- MySQL Full-text Search를 위한 인덱스 생성
-- ngram parser를 사용하여 한글 검색 지원

-- 1. 게시글 제목 검색용 인덱스
ALTER TABLE posts ADD FULLTEXT ft_idx_title (title) WITH PARSER ngram;

-- 2. 게시글 설명 검색용 인덱스
ALTER TABLE post_descriptions ADD FULLTEXT ft_idx_description (description) WITH PARSER ngram;

-- 3. 책 정보 검색용 인덱스 (제목, 저자)
ALTER TABLE books ADD FULLTEXT ft_idx_book_title (title) WITH PARSER ngram;
ALTER TABLE books ADD FULLTEXT ft_idx_book_author (author) WITH PARSER ngram;
-- 복합 인덱스도 생성 (제목+저자 동시 검색용)
ALTER TABLE books ADD FULLTEXT ft_idx_book_title_author (title, author) WITH PARSER ngram;

-- 4. 과목명 검색용 인덱스 (D안 준비)
ALTER TABLE subjects ADD FULLTEXT ft_idx_subject_name (subject_name) WITH PARSER ngram;

-- 5. 교수명 검색용 인덱스 (D안 준비)
ALTER TABLE professors ADD FULLTEXT ft_idx_professor_name (professor_name) WITH PARSER ngram;

-- 인덱스 생성 확인
SHOW INDEX FROM posts WHERE Key_name LIKE 'ft_%';
SHOW INDEX FROM post_descriptions WHERE Key_name LIKE 'ft_%';
SHOW INDEX FROM books WHERE Key_name LIKE 'ft_%';
SHOW INDEX FROM subjects WHERE Key_name LIKE 'ft_%';
SHOW INDEX FROM professors WHERE Key_name LIKE 'ft_%';

-- ngram 토큰 크기 확인 (기본값: 2)
SHOW VARIABLES LIKE 'ngram_token_size';