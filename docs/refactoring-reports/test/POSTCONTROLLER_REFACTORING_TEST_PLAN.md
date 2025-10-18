# PostController 리팩터링 테스트 계획

## 🎯 테스트 목표

PostController 리팩터링 후 **기존 기능의 100% 동일한 동작**을 검증하여 사용자에게 영향을 주지 않음을 확인합니다.

## 📋 테스트 범위

### 리팩터링된 메서드들
1. **list()** - 게시글 목록 조회 (119줄 → 25줄)
2. **editForm()** - 게시글 수정 폼 (63줄 → 23줄)  
3. **update()** - 게시글 수정 처리 (중복 코드 70줄 제거)
4. **createForm()** - 게시글 작성 폼 (일부 개선)

### 새로 생성된 컴포넌트들
- **PostSearchRequest** DTO
- **PostFormDataBuilder** 유틸리티
- **PostControllerHelper** 서비스

## 🧪 상세 테스트 시나리오

### 1. 게시글 목록 조회 (list 메서드) - 핵심 테스트

#### 1-1. 기본 목록 조회
```
테스트 URL: http://localhost:8080/posts
예상 결과:
- 기본 12개 게시글 표시
- 최신순 정렬 (createdAt DESC)
- 페이징 정상 동작
- BLOCKED 게시글 제외
- productTypes, statuses 선택 옵션 표시
```

#### 1-2. 페이징 테스트
```
테스트 URL들:
- /posts?page=0&size=12 (기본)
- /posts?page=1&size=12 (2페이지)
- /posts?page=0&size=6 (크기 변경)
- /posts?page=0&size=150 (크기 초과 → 12로 변경되는지)

확인 사항:
- 페이지 번호 정상 처리
- 크기 검증 (size > 100 → DEFAULT_PAGE_SIZE)
- 페이징 네비게이션 정상 표시
```

#### 1-3. 정렬 기능 테스트
```
테스트 URL들:
- /posts?sortBy=NEWEST (최신순)
- /posts?sortBy=PRICE_ASC (가격 낮은순)
- /posts?sortBy=PRICE_DESC (가격 높은순)  
- /posts?sortBy=VIEW_COUNT (조회수순)
- /posts?sortBy= (빈값 → NEWEST로 기본값 설정되는지)

확인 사항:
- 각 정렬 옵션별 결과 순서 확인
- sortBy 파라미터 UI에 올바르게 표시
```

#### 1-4. 검색 기능 테스트
```
테스트 URL들:
- /posts?search=Java (키워드 검색)
- /posts?search=  (빈 검색어)
- /posts?search=ab (2글자 미만)
- /posts?search=선형대수학 김선형 (복수 키워드)

확인 사항:
- Full-text 검색 정상 동작
- 검색어 있을 때 sortBy 기본값 = RELEVANCE
- 검색 키워드 하이라이팅 정상 표시
- 검색 결과 개수 표시
- 검색어가 UI 입력창에 유지되는지
```

#### 1-5. 필터링 테스트
```
테스트 URL들:
- /posts?productType=TEXTBOOK (교재만)
- /posts?status=AVAILABLE (판매중만)
- /posts?minPrice=10000&maxPrice=50000 (가격 범위)
- /posts?schoolId=1 (특정 학교)
- /posts?productType=TEXTBOOK&status=AVAILABLE&minPrice=10000 (복합 필터)

확인 사항:
- 각 필터 옵션이 결과에 정확히 반영
- 선택한 필터 값이 UI에 유지
- 복합 필터링 정상 동작
```

#### 1-6. 특정 조건 검색 테스트
```
테스트 URL들:
- /posts?subjectId=123 (특정 과목)
- /posts?professorId=456 (특정 교수)
- /posts?bookTitle=운영체제 (책 제목)

확인 사항:
- 페이지 제목이 동적으로 변경되는지
- "머신러닝실무 (김태령 교수님) 관련 게시글" 형태
- 검색 결과가 해당 조건에 맞는지
- 추가 필터링이 정상 동작하는지 (이전 이슈 재확인)
```

#### 1-7. 사용자별 기능 테스트
```
로그인 상태별 테스트:
- 비로그인: userSchoolId = null
- 로그인 (학교 정보 있음): userSchoolId 설정
- 로그인 (학교 정보 없음): userSchoolId = null, 예외 없이 처리

확인 사항:
- 학교 정보 조회 실패 시 정상 처리
- 로그 메시지 정상 출력
- 기능에 영향 없음
```

### 2. 게시글 수정 폼 (editForm 메서드)

#### 2-1. 정상 접근 테스트
```
테스트 URL: /posts/{id}/edit (본인 게시글)
확인 사항:
- 기존 데이터가 폼에 정확히 로드
- productTypes, transactionMethods, statuses 옵션 표시
- selectedBookJson 정상 설정
- selectedSubjectJson 정상 설정
- isEdit = true 설정
- maxImages 값 설정
```

#### 2-2. 권한 테스트
```
테스트 케이스:
1. 본인 게시글 (AVAILABLE) → 접근 허용
2. 본인 게시글 (BLOCKED) → 접근 허용 (작성자)
3. 타인 게시글 → 접근 거부
4. 관리자가 BLOCKED 게시글 → 접근 허용
5. 비로그인 → 로그인 페이지 리다이렉트

확인 사항:
- authorizationService.requireCanEdit() 정상 동작
- 권한 없을 시 적절한 예외 메시지
```

#### 2-3. JSON 데이터 테스트
```
테스트 케이스:
1. 책 정보 있는 게시글 → selectedBookJson 정상 생성
2. 책 정보 없는 게시글 → selectedBookJson = "null"
3. 과목 정보 있는 게시글 → selectedSubjectJson 정상 생성
4. 과목 정보 없는 게시글 → selectedSubjectJson = "null"
5. JSON 변환 오류 시 → "null" 반환, 로그 출력

확인 사항:
- PostFormDataBuilder.buildBookJson() 정상 동작
- PostFormDataBuilder.buildSubjectJson() 정상 동작
- 기존 로직과 완전 동일한 JSON 구조
```

### 3. 게시글 수정 처리 (update 메서드)

#### 3-1. 정상 수정 테스트
```
테스트 데이터:
- 기본 정보 수정 (제목, 설명, 가격)
- 이미지 추가/삭제/순서 변경
- 책 정보 변경
- 과목 정보 변경
- 상태 변경

확인 사항:
- 수정 성공 시 상세 페이지로 리다이렉트
- successMessage 플래시 메시지 표시
- 모든 변경사항이 정확히 저장
```

#### 3-2. 검증 오류 테스트 (BindingResult)
```
테스트 케이스:
- 필수 필드 누락 (제목, 가격 등)
- 최대 이미지 개수 초과
- 잘못된 데이터 타입

확인 사항:
- 폼으로 다시 돌아감
- 에러 메시지 표시
- PostFormDataBuilder.addFormAttributesForError() 정상 동작
- 기존 데이터 유지 (selectedBookJson, selectedSubjectJson)
```

#### 3-3. 비즈니스 검증 오류 테스트 (ValidationException)
```
테스트 케이스:
- 파일 크기 초과
- 지원하지 않는 파일 형식
- 기타 비즈니스 규칙 위반

확인 사항:
- 폼으로 다시 돌아감  
- global 에러 메시지 표시
- PostFormDataBuilder.addFormAttributesForError() 정상 동작
- 기존 데이터 유지
```

#### 3-4. 시스템 오류 테스트 (Exception)
```
테스트 케이스:
- DB 연결 오류 (시뮬레이션)
- 파일 시스템 오류

확인 사항:
- 에러 페이지로 리다이렉트
- errorMessage 플래시 메시지 표시
- 로그 출력 정상
```

### 4. 게시글 작성 폼 (createForm 메서드)

#### 4-1. 정상 접근 테스트
```
테스트 URL: /posts/new
확인 사항:
- 빈 PostRequestDto 생성
- 빈 Post 객체 생성 (postImages = 빈 리스트)
- productTypes, transactionMethods 옵션 표시
- selectedBookJson = "null"
- selectedSubjectJson = "null" (추가됨)
- isEdit = false
- maxImages 설정
```

#### 4-2. 권한 테스트
```
테스트 케이스:
1. 비로그인 → /login?returnUrl=/posts/new 리다이렉트
2. 로그인 + 이메일 미인증 → /verification-required?returnUrl=/posts/new
3. 로그인 + 이메일 인증 완료 → 폼 표시

확인 사항:
- 각 단계별 적절한 리다이렉트
- 경고 로그 출력
```

### 5. 기타 기능 연동 테스트

#### 5-1. 다른 기능과의 연동
```
테스트 케이스:
1. 게시글 목록 → 상세 페이지 링크
2. 게시글 목록 → 수정 폼 링크  
3. 수정 완료 → 상세 페이지 리다이렉트
4. 검색 → 상세 페이지 → 뒤로가기 (검색 조건 유지)

확인 사항:
- 모든 링크 정상 동작
- 페이지 간 데이터 전달 정상
- 브라우저 뒤로가기 정상
```

#### 5-2. AJAX 기능 연동
```
테스트 케이스:
1. 책 검색 API 호출 (폼에서)
2. 과목 검색 API 호출 (폼에서)
3. 찜 기능 (목록에서)
4. 상태 변경 API

확인 사항:
- AJAX 요청 정상 처리
- 응답 데이터 정상 반영
- 에러 처리 정상
```

## 🔍 중점 확인 사항

### 1. 데이터 정확성
- [ ] 모든 Model 속성이 템플릿에 정확히 전달
- [ ] 검색 조건이 UI에 정확히 표시/유지
- [ ] JSON 데이터 구조가 기존과 동일
- [ ] 페이징 정보 정확성

### 2. 성능 영향
- [ ] 페이지 로딩 시간 변화 없음
- [ ] 메모리 사용량 급증 없음
- [ ] 데이터베이스 쿼리 횟수 동일

### 3. 오류 처리
- [ ] 예외 상황에서 적절한 에러 페이지/메시지
- [ ] 로그 출력 정상 (레벨, 메시지 내용)
- [ ] 시스템 안정성 유지

### 4. UI/UX 일관성
- [ ] 기존 디자인/레이아웃 유지
- [ ] 버튼/링크 동작 동일
- [ ] 폼 검증 메시지 표시 동일
- [ ] 페이지 제목/설명 정확성

## 📋 테스트 체크리스트

### Phase 1: 기본 기능 테스트
- [ ] 게시글 목록 기본 조회 (/posts)
- [ ] 페이징 동작 (page, size 파라미터)
- [ ] 정렬 기능 (sortBy 파라미터)
- [ ] 검색 기능 (search 파라미터)
- [ ] 기본 필터링 (productType, status, price 범위)
- [ ] 특정 조건 검색 (subjectId, professorId, bookTitle)

### Phase 2: 폼 기능 테스트  
- [ ] 게시글 작성 폼 접근 (/posts/new)
- [ ] 게시글 수정 폼 접근 (/posts/{id}/edit)
- [ ] 폼 데이터 로딩 (기존 데이터, JSON 설정)
- [ ] 권한 체크 정상 동작

### Phase 3: 수정 처리 테스트
- [ ] 정상 수정 처리
- [ ] 검증 오류 처리 (BindingResult)
- [ ] 비즈니스 오류 처리 (ValidationException)  
- [ ] 시스템 오류 처리 (Exception)

### Phase 4: 통합 테스트
- [ ] 다른 페이지와의 연동
- [ ] AJAX 기능 연동
- [ ] 브라우저 뒤로가기/앞으로가기
- [ ] 세션 처리 정상

### Phase 5: 엣지 케이스 테스트
- [ ] 잘못된 파라미터 처리
- [ ] 존재하지 않는 ID 처리
- [ ] 동시 접근 상황
- [ ] 대용량 데이터 처리

## 🚨 알려진 이슈 재확인

### 이전에 발견된 문제들
1. **과목/책 선택 후 상세 필터 적용 시 전체 게시글에서 필터링되는 문제**
   - 이 문제가 리팩터링으로 해결되었는지 확인
   - 머신러닝실무 과목 → 거래완료 필터 테스트

2. **UI 상태 유지 문제**  
   - 거래 상태 필드가 검색 후 UI에 유지되는지 확인
   - 다른 필드들도 정상 유지되는지 확인

## 📊 테스트 결과 기록 양식

```
테스트 항목: [항목명]
테스트 URL: [URL]
테스트 데이터: [입력값]
예상 결과: [기대값]
실제 결과: [실제값]
상태: [ ] PASS / [ ] FAIL
비고: [특이사항]
```

## 🔧 테스트 도구

### 브라우저 테스트
- **Chrome DevTools**: 네트워크, 콘솔 로그 확인
- **Firefox**: 크로스 브라우저 호환성
- **모바일 뷰**: 반응형 디자인 확인

### 개발자 도구
- **IntelliJ 로그**: 애플리케이션 로그 모니터링
- **Database 확인**: 데이터 정확성 검증
- **메모리 프로파일러**: 성능 영향 측정

## 🎯 성공 기준

- **모든 기존 기능이 100% 동일하게 동작**
- **새로운 오류나 예외 상황 발생하지 않음**
- **성능 저하 없음 (응답시간, 메모리 사용량)**
- **UI/UX 변화 없음**
- **로그 메시지 정상 출력**

이 테스트 계획을 바탕으로 체계적으로 검증을 진행하여 리팩터링의 안전성을 확인해주세요!