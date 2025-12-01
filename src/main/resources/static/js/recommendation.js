/**
 * 추천 시스템 JavaScript
 * 기존 UI 구조와 완벽히 일치하도록 재작성
 */

// CSRF 토큰 가져오기
function getCsrfToken() {
  return document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
}

function getCsrfHeader() {
  return document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
}

// ======== 세션 관리 시스템 ========
const SessionManager = {
  SESSION_KEY: 'unibook_rec_session',
  SESSION_DURATION: 24 * 60 * 60 * 1000, // 24시간 (밀리초)

  /**
   * 세션 ID 생성
   * UUID v4 형식 생성
   */
  generateSessionId() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      const r = Math.random() * 16 | 0;
      const v = c === 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  },

  /**
   * 현재 세션 정보 가져오기
   * @returns {Object} { sessionId, expiresAt }
   */
  getSession() {
    try {
      const sessionData = localStorage.getItem(this.SESSION_KEY);
      if (!sessionData) {
        return this.createNewSession();
      }

      const session = JSON.parse(sessionData);
      const now = Date.now();

      // 세션 만료 체크
      if (session.expiresAt < now) {
        console.debug('세션 만료, 새로운 세션 생성');
        return this.createNewSession();
      }

      return session;
    } catch (error) {
      console.error('세션 조회 실패:', error);
      return this.createNewSession();
    }
  },

  /**
   * 새로운 세션 생성 및 저장
   * @returns {Object} { sessionId, expiresAt }
   */
  createNewSession() {
    const session = {
      sessionId: this.generateSessionId(),
      expiresAt: Date.now() + this.SESSION_DURATION
    };

    try {
      localStorage.setItem(this.SESSION_KEY, JSON.stringify(session));
      console.debug('새로운 세션 생성:', session.sessionId);
    } catch (error) {
      console.error('세션 저장 실패:', error);
    }

    return session;
  },

  /**
   * 세션 ID 가져오기 (간편 접근 메서드)
   * @returns {string}
   */
  getSessionId() {
    return this.getSession().sessionId;
  },

  /**
   * 세션 갱신 (만료 시간 연장)
   */
  renewSession() {
    const session = this.getSession();
    session.expiresAt = Date.now() + this.SESSION_DURATION;

    try {
      localStorage.setItem(this.SESSION_KEY, JSON.stringify(session));
      console.debug('세션 갱신:', session.sessionId);
    } catch (error) {
      console.error('세션 갱신 실패:', error);
    }
  },

  /**
   * 세션 삭제 (테스트용 또는 로그아웃 시)
   */
  clearSession() {
    try {
      localStorage.removeItem(this.SESSION_KEY);
      console.debug('세션 삭제 완료');
    } catch (error) {
      console.error('세션 삭제 실패:', error);
    }
  }
};

/**
 * 맞춤 추천 로드 (메인 페이지용)
 */
function loadPersonalizedRecommendations(limit = 10) {
  const wrapper = document.getElementById('recommendations-wrapper');
  if (!wrapper) return;

  // 로딩 상태 표시
  wrapper.innerHTML = `
    <div class="text-center py-5">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">로딩 중...</span>
      </div>
      <p class="mt-3 text-muted">추천 게시글을 불러오는 중...</p>
    </div>
  `;

  // API 호출
  fetch(`/api/recommendations/for-you?limit=${limit}`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      [getCsrfHeader()]: getCsrfToken()
    }
  })
  .then(response => response.json())
  .then(data => {
    if (data.success && data.recommendations && data.recommendations.length > 0) {
      renderMainPageRecommendations(wrapper, data.recommendations);

      // 노출 추적 (5분 중복 윈도우는 서버에서 처리)
      trackRecommendationImpression('FOR_YOU', data.recommendations.length, 'main', null);
    } else {
      // 추천할 게시글이 없을 때
      wrapper.innerHTML = `
        <div class="text-center py-5">
          <i class="bi bi-inbox text-muted" style="font-size: 3rem;"></i>
          <p class="mt-3 text-muted">추천할 게시글이 없습니다</p>
        </div>
      `;
    }
  })
  .catch(error => {
    console.error('추천 로드 실패:', error);
    wrapper.innerHTML = `
      <div class="alert alert-danger" role="alert">
        <i class="bi bi-exclamation-triangle"></i>
        추천 목록을 불러오는 중 오류가 발생했습니다.
      </div>
    `;
  });
}

/**
 * 비슷한 게시글 로드 (상세 페이지용)
 */
function loadSimilarPosts(postId, limit = 6) {
  const container = document.getElementById('similar-posts-container');
  if (!container) return;

  // 로딩 상태 표시
  container.innerHTML = `
    <div class="text-center py-3">
      <div class="spinner-border spinner-border-sm text-primary" role="status">
        <span class="visually-hidden">로딩 중...</span>
      </div>
    </div>
  `;

  // API 호출
  fetch(`/api/recommendations/similar/${postId}?limit=${limit}`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      [getCsrfHeader()]: getCsrfToken()
    }
  })
  .then(response => response.json())
  .then(data => {
    if (data.success && data.similarPosts && data.similarPosts.length > 0) {
      renderDetailPageSimilarPosts(container, data.similarPosts);

      // 노출 추적 (5분 중복 윈도우는 서버에서 처리)
      trackRecommendationImpression('SIMILAR', data.similarPosts.length, 'detail', postId);
    } else {
      container.innerHTML = `
        <p class="text-muted text-center">비슷한 게시글이 없습니다</p>
      `;
    }
  })
  .catch(error => {
    console.error('비슷한 게시글 로드 실패:', error);
    container.innerHTML = `
      <p class="text-danger text-center">
        <i class="bi bi-exclamation-triangle"></i>
        불러오기 실패
      </p>
    `;
  });
}

/**
 * 메인 페이지 추천 렌더링 (index.html의 최근 거래 게시글과 동일한 구조)
 */
function renderMainPageRecommendations(wrapper, posts) {
  const cardsHTML = posts.map((post, index) => createMainPageCard(post, index)).join('');
  wrapper.innerHTML = `
    <div class="posts-carousel">
      <!-- 캐러셀 네비게이션 버튼 -->
      <div class="carousel-nav prev">
        <button type="button" onclick="moveRecommendationCarousel(-1)" aria-label="이전">
          <i class="bi bi-chevron-left"></i>
        </button>
      </div>
      <div class="carousel-nav next">
        <button type="button" onclick="moveRecommendationCarousel(1)" aria-label="다음">
          <i class="bi bi-chevron-right"></i>
        </button>
      </div>

      <!-- 캐러셀 컨테이너 -->
      <div class="carousel-container" id="recommendationsCarousel">
        ${cardsHTML}
      </div>

      <!-- 캐러셀 인디케이터 -->
      <div class="carousel-indicators" id="recommendationIndicators"></div>
    </div>
  `;

  // 캐러셀 초기화
  initRecommendationCarousel();
}

/**
 * 상세 페이지 비슷한 게시글 렌더링 (detail.html의 같은 과목 구조)
 */
function renderDetailPageSimilarPosts(container, posts) {
  // 현재 게시글 ID 가져오기
  const sourcePostId = document.getElementById('current-post-id')?.value || null;
  const cardsHTML = posts.map((post, index) => createDetailPageCard(post, index, sourcePostId)).join('');
  // container 자체가 swiper-wrapper이므로 바로 카드들만 넣음
  container.innerHTML = cardsHTML;

  // Swiper 재초기화 (기존 subject-swiper와 동일한 설정)
  setTimeout(() => {
    const swiperElement = document.querySelector('.similar-swiper');
    if (swiperElement && typeof Swiper !== 'undefined') {
      // 기존 Swiper 인스턴스 파괴
      if (swiperElement.swiper) {
        swiperElement.swiper.destroy(true, true);
      }

      // 새 Swiper 생성
      new Swiper('.similar-swiper', {
        slidesPerView: 1,
        spaceBetween: 20,
        navigation: {
          nextEl: '.similar-next',
          prevEl: '.similar-prev',
        },
        breakpoints: {
          576: {
            slidesPerView: 2,
          },
          768: {
            slidesPerView: 3,
          },
          1024: {
            slidesPerView: 4,
          }
        },
        observer: true,
        observeParents: true
      });
    }
  }, 150);
}

/**
 * 메인 페이지 카드 생성 (index.html post-card 구조와 동일)
 */
function createMainPageCard(post, position) {
  const hasImage = post.images && post.images.length > 0;
  const imagePath = hasImage ? post.images[0].imagePath : '';
  const statusInfo = getStatusInfo(post.status);
  const productTypeIcon = getProductTypeIcon(post.productType);
  const formattedPrice = formatPrice(post.price);
  const formattedDate = formatDate(post.createdAt);
  const schoolName = post.user?.schoolName || '학교 정보 없음';

  return `
    <div class="post-card" onclick="trackRecommendationClick(${post.postId}, 'FOR_YOU', ${position}); window.location.href='/posts/${post.postId}'">
      <div class="post-card-image ${hasImage ? 'has-image' : ''}">
        <!-- 상태 배지 -->
        <span class="status-badge ${statusInfo.className}">
          <i class="bi ${statusInfo.icon}"></i>
          <span>${statusInfo.text}</span>
        </span>
        <!-- 게시글 이미지가 있는 경우 -->
        ${hasImage ? `
          <img src="${imagePath}"
               alt="게시글 이미지"
               onerror="this.style.display='none'; this.nextElementSibling.style.display='block';">
        ` : ''}
        <!-- 이미지가 없거나 로드 실패시 상품 타입 아이콘 -->
        <div class="product-type-icon" ${hasImage ? 'style="display:none"' : ''}>
          <i class="bi ${productTypeIcon}"></i>
        </div>
      </div>
      <div class="post-card-body">
        <h5 class="post-card-title">${escapeHtml(post.title)}</h5>
        <div class="post-card-meta">
          <div class="mb-1">
            <i class="bi bi-geo-alt-fill me-1"></i>
            <span>${escapeHtml(schoolName)}</span>
          </div>
          <div>
            <i class="bi bi-clock me-1"></i>
            <span>${formattedDate}</span>
          </div>
        </div>
        <div class="post-card-footer">
          <div class="post-card-price">
            <span>${formattedPrice}</span>원
          </div>
        </div>
      </div>
    </div>
  `;
}

/**
 * 상세 페이지 카드 생성 (detail.html related-card 구조와 동일)
 */
function createDetailPageCard(post, position, sourcePostId) {
  const hasImage = post.images && post.images.length > 0;
  const imagePath = hasImage ? post.images[0].imagePath : '';
  const statusInfo = getStatusInfo(post.status);
  const formattedPrice = formatPrice(post.price);
  const formattedDate = formatDetailDate(post.createdAt);

  return `
    <div class="swiper-slide">
      <a href="/posts/${post.postId}" class="text-decoration-none" onclick="trackRecommendationClick(${post.postId}, 'SIMILAR', ${position}, ${sourcePostId}); return true;">
        <div class="related-card">
          <!-- 이미지 -->
          <div class="related-card-image">
            <!-- 상태 배지 -->
            <span class="status-badge ${statusInfo.className}">
              <i class="bi ${statusInfo.icon}"></i>
              <span>${statusInfo.text}</span>
            </span>
            ${hasImage ? `
              <img src="${imagePath}" alt="${escapeHtml(post.title)}">
            ` : `
              <div class="h-100 d-flex align-items-center justify-content-center">
                <i class="bi bi-image text-muted" style="font-size: 3rem;"></i>
              </div>
            `}
          </div>

          <!-- 카드 내용 -->
          <div class="related-card-body">
            <h6 class="related-card-title text-dark">${escapeHtml(post.title)}</h6>
            <p class="related-card-price mb-2">${formattedPrice}원</p>

            <!-- 메타 정보 -->
            <div class="meta-info">
              ${post.subject ? `
                <div class="mb-1">
                  <i class="bi bi-book-fill"></i>
                  <span class="post-subject-name">${escapeHtml(post.subject.subjectName)}</span>
                  ${post.subject.professor ? `
                    · <span class="post-professor-name">${escapeHtml(post.subject.professor.professorName)}</span>
                  ` : ''}
                </div>
              ` : ''}
              ${post.book ? `
                <div class="mb-1">
                  <i class="bi bi-journal-bookmark-fill"></i>
                  <span class="post-book-title">${escapeHtml(post.book.title)}</span>
                </div>
              ` : ''}
              ${post.user?.department?.school ? `
                <div class="mb-1">
                  <i class="bi bi-building-fill"></i>
                  <span class="post-school-name">${escapeHtml(post.user.department.school.schoolName)}</span>
                </div>
              ` : ''}
            </div>

            <!-- 하단 정보 -->
            <div class="card-footer-info">
              <small class="text-muted">
                <i class="bi bi-eye-fill"></i> <span>${post.viewCount || 0}</span>
                <span class="ms-2">
                  <i class="bi bi-heart"></i>
                  <span class="wishlist-count">${post.wishlistCount || 0}</span>
                </span>
              </small>
              <small class="text-muted">${formattedDate}</small>
            </div>
          </div>
        </div>
      </a>
    </div>
  `;
}

/**
 * 상태 정보 반환
 */
function getStatusInfo(status) {
  const statusMap = {
    'AVAILABLE': { className: 'available', icon: 'bi-check-circle', text: '판매중' },
    'RESERVED': { className: 'reserved', icon: 'bi-clock', text: '예약중' },
    'COMPLETED': { className: 'completed', icon: 'bi-check-square', text: '거래완료' },
    'BLOCKED': { className: 'blocked', icon: 'bi-exclamation-triangle', text: '차단됨' }
  };
  return statusMap[status] || statusMap['AVAILABLE'];
}

/**
 * 상품 타입 아이콘 반환
 */
function getProductTypeIcon(productType) {
  const iconMap = {
    'TEXTBOOK': 'bi-book',
    'CERTBOOK': 'bi-award',
    'NOTE': 'bi-journal-text',
    'PASTEXAM': 'bi-file-earmark-text'
  };
  return iconMap[productType] || 'bi-box';
}

/**
 * 가격 포맷팅
 */
function formatPrice(price) {
  return price.toLocaleString('ko-KR');
}

/**
 * 날짜 포맷팅 (메인 페이지용: MM월 dd일)
 */
function formatDate(dateString) {
  const date = new Date(dateString);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${month}월 ${day}일`;
}

/**
 * 날짜 포맷팅 (상세 페이지용: MM.dd HH:mm)
 */
function formatDetailDate(dateString) {
  const date = new Date(dateString);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  return `${month}.${day} ${hours}:${minutes}`;
}

/**
 * HTML 이스케이프
 */
function escapeHtml(text) {
  if (!text) return '';
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

// ======== 추천 캐러셀 관련 변수 및 함수 ========
let recCurrentSlide = 0;
let recTotalSlides = 0;
let recItemsPerView = 4;
let recAutoplayInterval;
let recIsTransitioning = false;

/**
 * 추천 캐러셀 초기화
 */
function initRecommendationCarousel() {
  const carousel = document.getElementById('recommendationsCarousel');
  if (!carousel) return;

  const items = carousel.querySelectorAll('.post-card');
  recTotalSlides = items.length;

  // 화면 크기에 따라 보여줄 아이템 수 결정
  updateRecItemsPerView();

  // 인디케이터 생성
  createRecIndicators();

  // 자동 재생 시작
  startRecAutoplay();

  // 화면 크기 변경 감지
  window.addEventListener('resize', () => {
    updateRecItemsPerView();
    updateRecCarouselPosition();
  });
}

/**
 * 화면 크기에 따른 아이템 수 업데이트
 */
function updateRecItemsPerView() {
  const width = window.innerWidth;
  if (width < 576) {
    recItemsPerView = 1;
  } else if (width < 768) {
    recItemsPerView = 2;
  } else if (width < 992) {
    recItemsPerView = 3;
  } else {
    recItemsPerView = 4;
  }
}

/**
 * 인디케이터 생성
 */
function createRecIndicators() {
  const indicatorsContainer = document.getElementById('recommendationIndicators');
  if (!indicatorsContainer) return;

  indicatorsContainer.innerHTML = '';
  const totalPages = Math.ceil(recTotalSlides / recItemsPerView);

  for (let i = 0; i < totalPages; i++) {
    const indicator = document.createElement('button');
    indicator.className = 'carousel-indicator' + (i === 0 ? ' active' : '');
    indicator.onclick = () => goToRecSlide(i);
    indicatorsContainer.appendChild(indicator);
  }
}

/**
 * 캐러셀 이동
 */
function moveRecommendationCarousel(direction) {
  if (recIsTransitioning) return;

  const totalPages = Math.ceil(recTotalSlides / recItemsPerView);
  recCurrentSlide = (recCurrentSlide + direction + totalPages) % totalPages;
  updateRecCarouselPosition();

  // 자동 재생 재시작
  stopRecAutoplay();
  startRecAutoplay();
}

/**
 * 특정 슬라이드로 이동
 */
function goToRecSlide(index) {
  if (recIsTransitioning) return;

  recCurrentSlide = index;
  updateRecCarouselPosition();

  // 자동 재생 재시작
  stopRecAutoplay();
  startRecAutoplay();
}

/**
 * 캐러셀 위치 업데이트
 */
function updateRecCarouselPosition() {
  const carousel = document.getElementById('recommendationsCarousel');
  if (!carousel) return;

  recIsTransitioning = true;
  const offset = -(recCurrentSlide * 100);
  carousel.style.transform = `translateX(${offset}%)`;

  // 인디케이터 업데이트
  updateRecIndicators();

  setTimeout(() => {
    recIsTransitioning = false;
  }, 500);
}

/**
 * 인디케이터 업데이트
 */
function updateRecIndicators() {
  const indicators = document.querySelectorAll('#recommendationIndicators .carousel-indicator');
  indicators.forEach((indicator, index) => {
    indicator.classList.toggle('active', index === recCurrentSlide);
  });
}

/**
 * 자동 재생 시작
 */
function startRecAutoplay() {
  recAutoplayInterval = setInterval(() => {
    moveRecommendationCarousel(1);
  }, 5000); // 5초마다
}

/**
 * 자동 재생 중지
 */
function stopRecAutoplay() {
  if (recAutoplayInterval) {
    clearInterval(recAutoplayInterval);
  }
}

/**
 * 추천 클릭 추적
 * @param {number} postId - 클릭된 게시글 ID
 * @param {string} type - 추천 타입 ("FOR_YOU" 또는 "SIMILAR")
 * @param {number} position - 추천 목록 내 위치 (0부터 시작)
 * @param {number|null} sourcePostId - SIMILAR 타입일 경우 기준 게시글 ID
 */
function trackRecommendationClick(postId, type, position, sourcePostId = null) {
  const url = '/api/recommendations/track-click';
  const payload = {
    postId: postId,
    type: type,
    position: position,
    sourcePostId: sourcePostId
  };
  const body = JSON.stringify(payload);

  // 1) sendBeacon 우선 시도
  if (navigator.sendBeacon) {
    const queued = navigator.sendBeacon(
      url,
      new Blob([body], { type: 'application/json' })
    );
    if (queued) {
      return;
    }
  }

  // 2) keepalive fetch 폴백
  const csrfToken = getCsrfToken();
  const csrfHeader = getCsrfHeader();
  const headers = { 'Content-Type': 'application/json' };
  if (csrfToken && csrfHeader) {
    headers[csrfHeader] = csrfToken;
  }

  fetch(url, {
    method: 'POST',
    headers: headers,
    body: body,
    keepalive: true
  }).catch(err => {
    // 실패해도 사용자에게 영향 없음
    console.debug('클릭 추적 실패:', err);
  });
}

/**
 * 추천 노출 추적
 * @param {string} type - 추천 타입 ("FOR_YOU" 또는 "SIMILAR")
 * @param {number} count - 노출된 추천 개수
 * @param {string} pageType - 페이지 타입 ("main", "detail" 등)
 * @param {number|null} sourcePostId - SIMILAR 타입일 경우 기준 게시글 ID
 */
function trackRecommendationImpression(type, count, pageType, sourcePostId = null) {
  if (count <= 0) return;

  const sessionId = SessionManager.getSessionId();
  const csrfToken = getCsrfToken();
  const csrfHeader = getCsrfHeader();

  // 비동기 전송 (사용자 경험에 영향 없음)
  fetch('/api/recommendations/track-impression', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      [csrfHeader]: csrfToken
    },
    body: JSON.stringify({
      sessionId: sessionId,
      type: type,
      count: count,
      pageType: pageType,
      sourcePostId: sourcePostId
    })
  }).catch(err => {
    // 실패해도 사용자에게 영향 없음
    console.debug('노출 추적 실패:', err);
  });
}

// 페이지 로드 시 자동 실행
document.addEventListener('DOMContentLoaded', function() {
  // 메인 페이지 추천
  if (document.getElementById('recommendations-wrapper')) {
    loadPersonalizedRecommendations(10);
  }

  // 상세 페이지 비슷한 게시글
  const postIdElement = document.getElementById('current-post-id');
  if (postIdElement && document.getElementById('similar-posts-container')) {
    const postId = postIdElement.value;
    loadSimilarPosts(postId, 6);
  }
});
