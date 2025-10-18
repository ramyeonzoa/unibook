# 🗺️ Unibook 사용자 여정 지도 (User Journey Map)

## 📊 전체 사용자 여정 흐름도

```mermaid
flowchart TB
    Start([🌟 Unibook 방문]) --> UserType{어떤 사용자인가요?}
    
    %% 사용자 유형 분기
    UserType -->|교재 판매 희망| SellerPath[판매자 여정]
    UserType -->|교재 구매 희망| BuyerPath[구매자 여정]
    UserType -->|둘러보기| BrowsePath[방문자 여정]
    UserType -->|플랫폼 관리| AdminPath[관리자 여정]
    
    %% ===== 판매자 여정 =====
    SellerPath --> SignUp1[회원가입]
    SignUp1 --> SchoolSelect1[학교/학과 선택<br/>자동완성 검색]
    SchoolSelect1 --> EmailVerify1[대학 이메일 인증<br/>SMTP 발송]
    EmailVerify1 -->|인증 완료| CreatePost[게시글 작성]
    
    CreatePost --> PostType{상품 유형 선택}
    PostType -->|교재| BookSearch[네이버 도서 API<br/>자동 정보 입력]
    PostType -->|기타 학용품| ManualInput[수동 정보 입력]
    
    BookSearch --> SubjectLink[과목-교수 연결<br/>학교 내 제한]
    ManualInput --> SubjectLink
    
    SubjectLink --> ImageUpload[다중 이미지 업로드<br/>드래그&드롭]
    ImageUpload --> PriceSet[가격 설정]
    PriceSet --> PostComplete[게시 완료]
    
    PostComplete --> SellerWait{판매 대기}
    SellerWait -->|키워드 매칭| KeywordNotify[키워드 알림<br/>잠재 구매자 발견]
    SellerWait -->|채팅 요청| ChatRequest1[채팅 요청 수신<br/>SSE 실시간 알림]
    
    ChatRequest1 --> FirebaseChat1[Firebase 실시간 채팅]
    FirebaseChat1 --> ChatFeatures1[채팅 기능<br/>· 이미지 전송<br/>· 읽음 확인<br/>· 거래 상태 변경]
    
    ChatFeatures1 --> StatusChange{거래 진행}
    StatusChange -->|예약| Reserved[예약중 상태]
    StatusChange -->|완료| Completed[거래완료]
    StatusChange -->|취소| BackToWait[판매중으로]
    
    Reserved --> FinalTrade[실제 거래]
    FinalTrade --> Completed
    BackToWait --> SellerWait
    
    %% ===== 구매자 여정 =====
    BuyerPath --> SignUp2[회원가입/로그인]
    SignUp2 --> SchoolVerify[소속 학교 확인]
    SchoolVerify --> SearchStart{검색 시작}
    
    SearchStart -->|키워드 검색| FullTextSearch[Full-text 검색<br/>MySQL ngram]
    SearchStart -->|조건 검색| FilterSearch[필터 검색]
    SearchStart -->|탐색| BrowseList[목록 탐색]
    
    FullTextSearch --> SearchFeatures[검색 기능<br/>/ 제목/내용/책정보<br/> 과목명/교수명<br/>· 관련도순 정렬]
    FilterSearch --> FilterFeatures[필터 기능<br/>· 과목/교수/학과<br/>· 가격 범위<br/>· 상품 유형<br/>· 거래 상태]
    BrowseList --> QuickFilters[빠른 필터<br/>· 우리 학교<br/>· 우리 학과<br/>· 최신순/가격순]
    
    SearchFeatures --> ResultList[검색 결과]
    FilterFeatures --> ResultList
    QuickFilters --> ResultList
    
    ResultList --> ViewDetail[상세 보기]
    ViewDetail --> DetailFeatures[상세 페이지<br/>· 가격 시세 차트<br/>· 관련 게시글<br/>· 판매자 정보]
    
    DetailFeatures --> BuyerAction{구매자 행동}
    BuyerAction -->|관심 있음| AddWishlist[찜하기]
    BuyerAction -->|구매 희망| StartChat[채팅 시작]
    BuyerAction -->|문제 발견| Report[신고하기]
    
    AddWishlist --> WishlistNotify[알림 설정<br/>· 가격 변동 알림<br/>· 상태 변경 알림]
    WishlistNotify --> WaitForChange{변동 대기}
    WaitForChange -->|가격 인하| PriceAlert[가격 인하 알림<br/>이모지 표시]
    WaitForChange -->|상태 변경| StatusAlert[상태 변경 알림]
    
    PriceAlert --> ViewDetail
    StatusAlert --> ViewDetail
    
    StartChat --> FirebaseChat2[Firebase 실시간 채팅]
    FirebaseChat2 --> Negotiation[가격 협상]
    Negotiation --> DealDecision{거래 결정}
    DealDecision -->|합의| MakeDeal[거래 약속]
    DealDecision -->|포기| LeaveChat[나가기]
    
    MakeDeal --> ActualTrade[실제 거래]
    ActualTrade --> TradeComplete[거래 완료]
    
    %% ===== 방문자 여정 =====
    BrowsePath --> PublicView[공개 페이지 열람<br/>· 메인 페이지<br/>· 게시글 목록<br/>· 검색]
    PublicView --> LimitAlert[기능 제한 안내]
    LimitAlert --> PromptSignUp[회원가입 유도]
    PromptSignUp --> SignUp1
    
    %% ===== 관리자 여정 =====
    AdminPath --> AdminLogin[관리자 로그인]
    AdminLogin --> Dashboard[대시보드]
    
    Dashboard --> AdminFeatures{관리 기능}
    AdminFeatures -->|통계| Statistics[통계 확인<br/>· 실시간 차트<br/>· 거래 현황<br/>· 사용자 증가]
    AdminFeatures -->|신고 관리| ReportMgmt[신고 처리<br/>· 자동 블라인드<br/>· 상세 조사<br/>· 제재 결정]
    AdminFeatures -->|사용자 관리| UserMgmt[사용자 관리<br/>· 검색/필터<br/>· 정지/해제<br/>· 활동 내역]
    AdminFeatures -->|게시글 관리| PostMgmt[게시글 관리<br/>· 상태별 조회<br/>· 차단/해제<br/>· 일괄 처리]
    AdminFeatures -->|캐시 모니터링| CacheMgmt[캐시 상태<br/>· 히트율 확인<br/>· 성능 최적화]
    
    ReportMgmt --> ProcessReport{신고 처리}
    ProcessReport -->|3건 이상| AutoBlock[자동 차단]
    ProcessReport -->|검토 필요| ManualReview[수동 검토]
    
    UserMgmt --> UserAction{사용자 조치}
    UserAction -->|정지| SuspendUser[계정 정지<br/>기간 설정]
    UserAction -->|해제| UnsuspendUser[정지 해제]
    
    Report --> ReportProcess[신고 처리 프로세스<br/>· 사유 선택<br/>· 자동 집계]
    
    %% ===== 기술적 특징 표시 =====
    EmailVerify1 -.->|기술| TechSMTP[SMTP + 토큰]
    FullTextSearch -.->|기술| TechMySQL[MySQL Full-text<br/>ngram parser]
    FirebaseChat1 -.->|기술| TechFirebase[Firebase Firestore<br/>실시간 동기화]
    ChatRequest1 -.->|기술| TechSSE[Server-Sent Events<br/>단방향 푸시]
    BookSearch -.->|기술| TechNaverAPI[네이버 도서 API]
    ImageUpload -.->|기술| TechFireStorage[Firebase Storage]
    DetailFeatures -.->|기술| TechChartJS[Chart.js<br/>가격 시세 시각화]
    WishlistNotify -.->|기술| TechAsync[비동기 처리<br/>@Async]
    Dashboard -.->|기술| TechCache[Caffeine Cache<br/>성능 최적화]
    
    %% ===== 다크모드/반응형 =====
    Start -.->|UI/UX| DarkMode[다크모드<br/>Bootstrap 5]
    Start -.->|UI/UX| Responsive[반응형<br/>모바일 최적화]
    
    %% 스타일링
    classDef userType fill:#e3f2fd,stroke:#1976d2,stroke-width:3px,color:#000000
    classDef seller fill:#fff3e0,stroke:#f57c00,stroke-width:2px,color:#000000
    classDef buyer fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px,color:#000000
    classDef admin fill:#ffebee,stroke:#c62828,stroke-width:2px,color:#000000
    classDef tech fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px,stroke-dasharray: 5 5,color:#000000
    classDef feature fill:#fffde7,stroke:#f9a825,stroke-width:2px,color:#000000
    classDef action fill:#e1f5fe,stroke:#0277bd,stroke-width:2px,color:#000000
    classDef success fill:#c8e6c9,stroke:#388e3c,stroke-width:3px,color:#000000
    
    class Start,UserType userType
    class SellerPath,CreatePost,BookSearch,PostComplete seller
    class BuyerPath,SearchStart,ViewDetail,StartChat buyer
    class AdminPath,Dashboard,ReportMgmt admin
    class TechSMTP,TechMySQL,TechFirebase,TechSSE,TechNaverAPI,TechFireStorage,TechChartJS,TechAsync,TechCache tech
    class SearchFeatures,FilterFeatures,DetailFeatures,ChatFeatures1,AdminFeatures feature
    class SignUp1,SignUp2,EmailVerify1,AddWishlist,Report action
    class Completed,TradeComplete,Review success
```

## 🎯 주요 사용자 시나리오

### 1. 📚 **판매자 시나리오** (교재를 팔고 싶은 학생)
- **목표**: 사용하지 않는 교재를 필요한 후배에게 판매
- **Pain Points 해결**: 
  - 네이버 API로 책 정보 자동 입력 → 번거로움 감소
  - 과목-교수 연결로 정확한 타겟팅 → 빠른 판매
  - 키워드 알림으로 잠재 구매자 발견 → 판매 기회 증가

### 2. 🔍 **구매자 시나리오** (교재가 필요한 학생)
- **목표**: 수강 과목에 맞는 적절한 가격의 교재 구매
- **Pain Points 해결**:
  - Full-text 검색으로 쉬운 검색 → 시간 절약
  - 가격 시세 차트로 적정가 확인 → 합리적 구매
  - 찜 + 알림으로 놓치지 않기 → 기회 포착

### 3. 🔧 **관리자 시나리오** (플랫폼 운영자)
- **목표**: 안전하고 신뢰할 수 있는 거래 환경 조성
- **자동화 기능**:
  - 3건 이상 신고 시 자동 차단 → 신속한 대응
  - 실시간 통계 대시보드 → 데이터 기반 운영
  - 캐시 모니터링 → 성능 최적화

## 🚀 기술적 하이라이트

### 실시간 기능
- **Firebase Firestore**: P2P 실시간 채팅, 이미지 전송
- **Server-Sent Events**: 서버 → 클라이언트 푸시 알림
- **WebSocket 대체**: SSE로 단방향 통신 최적화

### 검색 최적화
- **MySQL Full-text Search**: 한글 검색 최적화 (ngram)
- **Projection DTO**: 메모리 효율적 검색 결과 처리
- **복합 인덱스**: 빈번한 쿼리 패턴 최적화

### 성능 개선
- **Caffeine Cache**: 학과/교수 정보 캐싱
- **N+1 쿼리 해결**: Fetch Join 전략
- **비동기 처리**: 알림 발송 논블로킹

### 사용자 경험
- **다크모드**: 시스템 설정 연동 + localStorage 저장
- **반응형 디자인**: 모바일 우선 설계
- **접근성**: WCAG 가이드라인 준수

## 📈 성과 지표

- **코드 품질**: PostController 80% 코드 감소 (252줄 → 50줄)
- **성능**: 평균 응답시간 245ms → 12ms (95% 개선)
- **확장성**: 마이크로서비스 전환 가능한 아키텍처
- **신뢰성**: 학교 이메일 인증으로 신원 보장

## 🔮 향후 발전 방향

1. **단기 (1-2개월)**
   - 모바일 앱 개발 (React Native)
   - 결제 시스템 통합
   - 무인택배함 연동

2. **중기 (6개월)**
   - AI 기반 가격 추천
   - 교재 대여 서비스
   - 전국 대학 확장

3. **장기 (1년+)**
   - 블록체인 거래 인증
   - 해외 대학 진출
   - 교육 콘텐츠 플랫폼화