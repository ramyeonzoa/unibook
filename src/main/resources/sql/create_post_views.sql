-- ============================================
-- 추천 시스템을 위한 게시글 조회 기록 테이블
-- ============================================
-- 작성일: 2025-11-01
-- 목적: 사용자 행동 데이터 수집 및 Collaborative Filtering
-- ============================================

CREATE TABLE IF NOT EXISTS post_views (
    view_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '조회 기록 ID',
    user_id BIGINT NULL COMMENT '사용자 ID (비로그인 시 NULL)',
    post_id BIGINT NOT NULL COMMENT '게시글 ID',
    viewed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '조회 시각',

    -- Foreign Keys
    CONSTRAINT fk_post_views_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_post_views_post FOREIGN KEY (post_id)
        REFERENCES posts(post_id) ON DELETE CASCADE,

    -- Indexes for Performance
    INDEX idx_post_views_user_id (user_id) COMMENT '사용자별 조회 기록 검색',
    INDEX idx_post_views_post_id (post_id) COMMENT '게시글별 조회 수 집계',
    INDEX idx_post_views_viewed_at (viewed_at) COMMENT '시간대별 분석',
    INDEX idx_post_views_user_viewed (user_id, viewed_at) COMMENT '사용자별 최근 조회 기록'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='게시글 조회 기록 (추천 시스템용)';

-- ============================================
-- 테이블 생성 확인 쿼리
-- ============================================
SELECT * FROM information_schema.tables
WHERE table_schema = 'unibook_db' AND table_name = 'post_views';

-- ============================================
-- 초기 데이터 확인
-- ============================================
-- SELECT COUNT(*) FROM post_views;
