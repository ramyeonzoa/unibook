# RAG 챗봇 시스템 구현 완료 (MVP)

## 📋 프로젝트 개요

**목표**: Unibook 플랫폼에 RAG (Retrieval-Augmented Generation) 기반 고객 지원 챗봇 구현
**기간**: 2025-10-18
**기술 스택**: LangChain4j, OpenAI GPT-5-nano, text-embedding-3-small
**상태**: MVP 완료 ✅

---

## 🏗️ 아키텍처

### RAG 파이프라인
```
사용자 질문 입력
    ↓
임베딩 생성 (text-embedding-3-small)
    ↓
벡터 유사도 검색 (In-Memory Store)
    ↓
상위 3개 FAQ 검색 (유사도 0.5 이상)
    ↓
프롬프트 구성 (검색된 FAQ + 질문)
    ↓
GPT-5-nano 답변 생성
    ↓
응답 반환 (답변 + 출처 + 링크)
```

### 기술 스택
- **LLM**: OpenAI GPT-5-nano
- **Embedding**: OpenAI text-embedding-3-small
- **Vector DB**: In-Memory Embedding Store (LangChain4j)
- **Framework**: Spring Boot 3.5.0, LangChain4j 0.35.0
- **Frontend**: Vanilla JavaScript (ES6)

---

## 📂 프로젝트 구조

### 백엔드
```
src/main/java/com/unibook/
├── controller/api/
│   └── ChatbotApiController.java          # REST API 엔드포인트
├── service/
│   ├── EmbeddingService.java              # FAQ 임베딩 & 유사도 검색
│   └── ChatbotService.java                # RAG 파이프라인 & GPT 호출
└── domain/dto/
    ├── ChatbotRequestDto.java
    ├── ChatbotResponseDto.java            # SourceInfo 포함
    └── ChatbotKnowledgeDto.java

src/main/resources/
├── chatbot/
│   └── rag_seed.json                      # 39개 FAQ 지식 베이스
└── application.yml                        # OpenAI 설정
```

### 프론트엔드
```
src/main/resources/static/
├── css/
│   └── chatbot-widget.css                 # 챗봇 위젯 스타일
└── js/
    └── chatbot-widget.js                  # 챗봇 위젯 로직

src/main/resources/templates/fragments/
└── header.html                            # CSS/JS 로드 (styles, scripts)
```

---

## 🔑 핵심 기능

### 1. FAQ 임베딩 (EmbeddingService)
- **서버 시작 시 자동 실행** (`@PostConstruct`)
- 39개 FAQ를 OpenAI Embedding API로 벡터화
- In-Memory Vector Store에 저장
- 코사인 유사도 검색 지원

### 2. RAG 파이프라인 (ChatbotService)
- 사용자 질문을 임베딩으로 변환
- 상위 3개 관련 FAQ 검색 (유사도 0.5 이상)
- FAQ를 Context로 구성하여 GPT-5-nano에 전달
- 자연스러운 답변 생성

### 3. REST API (ChatbotApiController)
- `POST /api/chatbot/ask` - 질문 & 답변
- `GET /api/chatbot/health` - 헬스체크
- 표준 응답 형식: `{ success, data, message }`

### 4. 프론트엔드 위젯 (chatbot-widget.js)
- **우측 하단 플로팅 버튼** (모든 사용자)
- 기존 Firebase 채팅과 동일한 디자인
- 추천 질문 3개 제공
- 참고한 정보 섹션 (카테고리 + 질문 + 링크)
- 다크 모드 완벽 지원

---

## ⚙️ 설정

### application.yml
```yaml
openai:
  api:
    key: ${OPENAI_API_KEY}
    model: gpt-5-nano
    embedding-model: text-embedding-3-small
    max-tokens: 2000
    timeout: 30
```

### GPT-5-nano 특이사항
1. **temperature 고정**: 1.0만 지원 (조정 불가)
2. **max_completion_tokens 사용**: `maxCompletionTokens()` 메서드
3. **응답 형식**: `Response<AiMessage>` → `aiMessage.text()`

---

## 🎨 UI/UX

### 챗봇 위젯
- **플로팅 버튼**: 보라색 그라데이션 원형 버튼
- **헤더**: 보라색 그라데이션 + 로봇 아이콘
- **메시지**: 사용자(보라색) / 봇(흰색)
- **참고한 정보**:
  - 카테고리 (굵게)
  - 질문 텍스트
  - 클릭 가능한 링크 (anchor가 있는 경우)
  - 외부 링크 아이콘 (`bi-box-arrow-up-right`)

### 추천 질문
1. "Unibook이 뭔가요?"
2. "어떻게 거래하나요?"
3. "안전한 거래 팁을 알려주세요"

---

## 📊 데이터

### rag_seed.json 구조
```json
[
  {
    "id": "service.intro",
    "category": "서비스 소개",
    "question": "Unibook이 무엇인가요?",
    "answer": "대학생 맞춤형 교재 거래 플랫폼입니다...",
    "anchors": ["/about", "/faq#serviceAccordion"],
    "keywords": ["유니북", "서비스", "소개"]
  }
]
```

**총 39개 FAQ**:
- 서비스 소개 (5개)
- 회원 관리 (4개)
- 게시글 작성 (5개)
- 거래 방법 (6개)
- 채팅 및 알림 (4개)
- 안전 거래 (5개)
- 신고 및 문의 (4개)
- 기타 (6개)

---

## 🐛 알려진 이슈 (MVP 단계)

### 1. Hallucination
- GPT-5-nano가 가끔 FAQ에 없는 내용을 추측함
- **개선 방안**: 프롬프트에 "FAQ에 없으면 모른다고 답변" 강조 (이미 적용)

### 2. 참고한 정보 UX
- **문제**: 참고한 정보가 3개씩 표시되어 답변보다 먼저 보임
- **개선 방안**:
  - 상위 1~2개만 표시
  - 접을 수 있는 아코디언 형태
  - 답변 아래로 이동

### 3. 검색 실패 시 기본 응답
- 유사도 0.5 미만인 경우 "답변을 찾지 못했습니다" 표시
- **개선 방안**: 유사도 임계값 조정 (0.4 ~ 0.5)

---

## 🚀 배포 및 운영

### 의존성
```gradle
implementation 'dev.langchain4j:langchain4j:0.35.0'
implementation 'dev.langchain4j:langchain4j-open-ai:0.35.0'
```

### 환경 변수
```yaml
# application-local.yml
openai:
  api:
    key: sk-proj-xxxxx  # OpenAI API Key
```

### 비용
- **Embedding**: text-embedding-3-small (~$0.00002/1K tokens)
- **LLM**: GPT-5-nano (가격 정보 확인 필요)
- **예상**: 질문 1개당 약 $0.001~0.003

---

## 📈 향후 개선 사항

### 기능 개선
1. **검색 품질**
   - 유사도 임계값 동적 조정
   - Hybrid Search (키워드 + 벡터)
   - Re-ranking 추가

2. **UX 개선**
   - 참고한 정보 개수 제한 (1~2개)
   - 타이핑 애니메이션
   - 대화 히스토리 저장

3. **성능 최적화**
   - 응답 스트리밍 (SSE)
   - 캐싱 (자주 묻는 질문)
   - 벡터 DB 외부화 (Pinecone, Weaviate 등)

### 관리 기능
1. **어드민 대시보드**
   - FAQ 추가/수정/삭제
   - 질문 로그 분석
   - 사용자 만족도 추적

2. **모니터링**
   - API 호출 횟수
   - 평균 응답 시간
   - 에러율 추적

---

## 🎯 결론

### 달성한 목표
✅ RAG 기반 챗봇 시스템 구현
✅ OpenAI GPT-5-nano 통합
✅ 39개 FAQ 지식 베이스 구축
✅ 기존 디자인과 일관된 UI
✅ 모든 사용자 접근 가능 (비로그인 포함)
✅ 다크 모드 지원
✅ 참고한 정보 링크 제공

### MVP 완성도
- **핵심 기능**: 100% 완료
- **UX 개선 여지**: 참고한 정보 섹션 최적화 필요
- **성능**: 양호 (응답 시간 4~5초)
- **확장성**: In-Memory → 외부 Vector DB 마이그레이션 고려

---

## 📝 참고 문서

- [LangChain4j 공식 문서](https://docs.langchain4j.dev/)
- [OpenAI API 문서](https://platform.openai.com/docs)
- [GPT-5-nano 특이사항](https://platform.openai.com/docs/models/gpt-5-nano)

---

**작성일**: 2025-10-18
**작성자**: Claude (with Human Developer)
**버전**: MVP 1.0
