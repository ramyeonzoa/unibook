/**
 * RAG 챗봇 위젯 JavaScript
 * 기존 Firebase 채팅과 동일한 UX 제공
 */

class ChatbotWidget {
  constructor() {
    this.isOpen = false;
    this.messages = [];
    this.isLoading = false;

    this.init();
  }

  /**
   * 위젯 초기화
   */
  init() {
    this.createWidget();
    this.attachEventListeners();

    // 환영 메시지 표시
    this.showWelcomeMessage();
  }

  /**
   * 위젯 HTML 생성
   */
  createWidget() {
    const widgetHTML = `
      <!-- 플로팅 버튼 -->
      <button class="chatbot-floating-btn" id="chatbotFloatingBtn" aria-label="챗봇 열기">
        <i class="bi bi-chat-dots-fill"></i>
      </button>

      <!-- 챗봇 위젯 -->
      <div class="chatbot-widget" id="chatbotWidget">
        <!-- 헤더 -->
        <div class="chatbot-header">
          <div class="chatbot-header-left">
            <div class="chatbot-avatar">
              <i class="bi bi-robot"></i>
            </div>
            <div>
              <h5>Unibook 도우미</h5>
              <small><i class="bi bi-circle-fill text-success" style="font-size: 8px;"></i> 온라인</small>
            </div>
          </div>
          <button class="chatbot-close-btn" id="chatbotCloseBtn" aria-label="챗봇 닫기">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>

        <!-- 메시지 영역 -->
        <div class="chatbot-messages" id="chatbotMessages">
          <!-- 메시지가 동적으로 추가됨 -->
        </div>

        <!-- 입력 영역 -->
        <div class="chatbot-input-container">
          <div class="d-flex align-items-end gap-2">
            <textarea
              class="chatbot-input"
              id="chatbotInput"
              placeholder="궁금한 점을 물어보세요..."
              rows="1"
              maxlength="500"></textarea>
            <button class="chatbot-send-btn" id="chatbotSendBtn" aria-label="전송">
              <i class="bi bi-send-fill"></i>
            </button>
          </div>
          <div class="mt-2 text-muted small">
            <i class="bi bi-info-circle me-1"></i>
            Enter로 전송, Shift+Enter로 줄바꿈
          </div>
        </div>
      </div>
    `;

    // body에 위젯 추가
    document.body.insertAdjacentHTML('beforeend', widgetHTML);
  }

  /**
   * 이벤트 리스너 연결
   */
  attachEventListeners() {
    const floatingBtn = document.getElementById('chatbotFloatingBtn');
    const closeBtn = document.getElementById('chatbotCloseBtn');
    const sendBtn = document.getElementById('chatbotSendBtn');
    const input = document.getElementById('chatbotInput');

    // 플로팅 버튼 클릭
    floatingBtn.addEventListener('click', () => this.toggleWidget());

    // 닫기 버튼 클릭
    closeBtn.addEventListener('click', () => this.closeWidget());

    // 전송 버튼 클릭
    sendBtn.addEventListener('click', () => this.handleSend());

    // Enter 키 처리
    input.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        this.handleSend();
      }
    });

    // 자동 높이 조절
    input.addEventListener('input', (e) => {
      e.target.style.height = 'auto';
      e.target.style.height = Math.min(e.target.scrollHeight, 100) + 'px';
    });
  }

  /**
   * 위젯 토글
   */
  toggleWidget() {
    if (this.isOpen) {
      this.closeWidget();
    } else {
      this.openWidget();
    }
  }

  /**
   * 위젯 열기
   */
  openWidget() {
    const widget = document.getElementById('chatbotWidget');
    const floatingBtn = document.getElementById('chatbotFloatingBtn');

    widget.classList.add('active');
    floatingBtn.style.display = 'none';
    this.isOpen = true;

    // 입력창 포커스
    setTimeout(() => {
      document.getElementById('chatbotInput').focus();
    }, 300);
  }

  /**
   * 위젯 닫기
   */
  closeWidget() {
    const widget = document.getElementById('chatbotWidget');
    const floatingBtn = document.getElementById('chatbotFloatingBtn');

    widget.classList.remove('active');
    floatingBtn.style.display = 'flex';
    this.isOpen = false;
  }

  /**
   * 환영 메시지 표시
   */
  showWelcomeMessage() {
    const messagesContainer = document.getElementById('chatbotMessages');

    const welcomeHTML = `
      <div class="chatbot-welcome">
        <i class="bi bi-robot"></i>
        <h6>안녕하세요! Unibook 도우미입니다</h6>
        <p>궁금한 점을 물어보세요</p>
      </div>

      <div class="chatbot-suggestions">
        <button class="chatbot-suggestion-btn" data-question="Unibook이 뭔가요?">
          <i class="bi bi-question-circle"></i>Unibook이 뭔가요?
        </button>
        <button class="chatbot-suggestion-btn" data-question="어떻게 거래하나요?">
          <i class="bi bi-arrow-left-right"></i>어떻게 거래하나요?
        </button>
        <button class="chatbot-suggestion-btn" data-question="안전한 거래 팁을 알려주세요">
          <i class="bi bi-shield-check"></i>안전한 거래 팁을 알려주세요
        </button>
      </div>
    `;

    messagesContainer.innerHTML = welcomeHTML;

    // 추천 질문 클릭 이벤트
    const suggestionBtns = messagesContainer.querySelectorAll('.chatbot-suggestion-btn');
    suggestionBtns.forEach(btn => {
      btn.addEventListener('click', () => {
        const question = btn.getAttribute('data-question');
        document.getElementById('chatbotInput').value = question;
        this.handleSend();
      });
    });
  }

  /**
   * 메시지 전송 처리
   */
  async handleSend() {
    const input = document.getElementById('chatbotInput');
    const message = input.value.trim();

    if (!message || this.isLoading) return;

    // 사용자 메시지 추가
    this.addMessage('user', message);

    // 입력창 초기화
    input.value = '';
    input.style.height = 'auto';

    // 로딩 표시
    this.showLoading();

    try {
      // API 호출
      const response = await this.sendToAPI(message);

      // 로딩 제거
      this.hideLoading();

      // 봇 응답 추가
      this.addMessage('bot', response.answer, response.sources);

    } catch (error) {
      console.error('챗봇 API 오류:', error);

      // 로딩 제거
      this.hideLoading();

      // 에러 메시지 표시
      this.addMessage('bot', '죄송합니다. 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
    }
  }

  /**
   * API 호출
   */
  async sendToAPI(question) {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    const headers = {
      'Content-Type': 'application/json'
    };

    if (csrfToken && csrfHeader) {
      headers[csrfHeader] = csrfToken;
    }

    const response = await fetch('/api/chatbot/ask', {
      method: 'POST',
      headers: headers,
      body: JSON.stringify({ question: question })
    });

    if (!response.ok) {
      throw new Error('API 호출 실패');
    }

    const data = await response.json();

    if (!data.success) {
      throw new Error(data.message || 'API 응답 오류');
    }

    return data.data;
  }

  /**
   * 메시지 추가
   */
  addMessage(type, content, sources = null) {
    const messagesContainer = document.getElementById('chatbotMessages');

    // 환영 메시지 제거
    const welcome = messagesContainer.querySelector('.chatbot-welcome');
    const suggestions = messagesContainer.querySelector('.chatbot-suggestions');
    if (welcome) welcome.remove();
    if (suggestions) suggestions.remove();

    // 메시지 객체 생성
    const messageObj = {
      type: type,
      content: content,
      sources: sources,
      timestamp: new Date()
    };

    this.messages.push(messageObj);

    // 메시지 HTML 생성
    const messageHTML = this.createMessageHTML(messageObj);

    // 메시지 추가
    messagesContainer.insertAdjacentHTML('beforeend', messageHTML);

    // 스크롤 이동
    this.scrollToBottom();
  }

  /**
   * 메시지 HTML 생성
   */
  createMessageHTML(message) {
    const isUser = message.type === 'user';
    const alignClass = isUser ? 'text-end' : 'text-start';
    const messageClass = isUser ? 'chatbot-message-user' : 'chatbot-message-bot';
    const timeString = this.formatTime(message.timestamp);
    const messageId = `msg-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;

    let sourcesHTML = '';
    if (message.sources && message.sources.length > 0) {
      const sourcesId = `sources-${messageId}`;
      sourcesHTML = `
        <div class="chatbot-sources-accordion">
          <button class="chatbot-sources-toggle" type="button" onclick="this.classList.toggle('active'); document.getElementById('${sourcesId}').classList.toggle('show')">
            <i class="bi bi-book me-1"></i>
            <span>참고한 정보 ${message.sources.length}개 보기</span>
            <i class="bi bi-chevron-down ms-auto chatbot-toggle-icon"></i>
          </button>
          <div class="chatbot-sources-content" id="${sourcesId}">
            ${message.sources.map(source => {
              const questionText = this.escapeHtml(source.question);
              const categoryText = this.escapeHtml(source.category);

              if (source.anchor) {
                return `
                  <a href="${source.anchor}" class="chatbot-source-item chatbot-source-link" target="_blank">
                    <strong>${categoryText}</strong>
                    ${questionText}
                    <i class="bi bi-box-arrow-up-right ms-1" style="font-size: 0.75rem;"></i>
                  </a>
                `;
              } else {
                return `
                  <div class="chatbot-source-item">
                    <strong>${categoryText}</strong>
                    ${questionText}
                  </div>
                `;
              }
            }).join('')}
          </div>
        </div>
      `;
    }

    return `
      <div class="chatbot-message-wrapper ${alignClass}">
        <div class="chatbot-message ${messageClass}">
          <div class="chatbot-message-content">
            ${this.escapeHtml(message.content)}
          </div>
          <div class="chatbot-message-info">
            <small>${timeString}</small>
          </div>
          ${sourcesHTML}
        </div>
      </div>
    `;
  }

  /**
   * 로딩 표시
   */
  showLoading() {
    this.isLoading = true;

    const messagesContainer = document.getElementById('chatbotMessages');
    const sendBtn = document.getElementById('chatbotSendBtn');

    const loadingHTML = `
      <div class="chatbot-message-wrapper text-start" id="chatbotLoading">
        <div class="chatbot-message chatbot-message-bot">
          <div class="chatbot-loading">
            <div class="chatbot-loading-dot"></div>
            <div class="chatbot-loading-dot"></div>
            <div class="chatbot-loading-dot"></div>
          </div>
        </div>
      </div>
    `;

    messagesContainer.insertAdjacentHTML('beforeend', loadingHTML);
    sendBtn.disabled = true;

    this.scrollToBottom();
  }

  /**
   * 로딩 제거
   */
  hideLoading() {
    this.isLoading = false;

    const loading = document.getElementById('chatbotLoading');
    const sendBtn = document.getElementById('chatbotSendBtn');

    if (loading) loading.remove();
    sendBtn.disabled = false;
  }

  /**
   * 스크롤 이동
   */
  scrollToBottom() {
    const container = document.getElementById('chatbotMessages');
    setTimeout(() => {
      container.scrollTop = container.scrollHeight;
    }, 100);
  }

  /**
   * 시간 포맷팅
   */
  formatTime(date) {
    return date.toLocaleTimeString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: true
    });
  }

  /**
   * HTML 이스케이프
   */
  escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }
}

// 페이지 로드 시 챗봇 초기화
document.addEventListener('DOMContentLoaded', function() {
  // 모든 사용자에게 챗봇 표시 (비로그인 사용자에게 특히 유용)
  window.chatbotWidget = new ChatbotWidget();
  console.log('RAG 챗봇 위젯 초기화 완료');
});
