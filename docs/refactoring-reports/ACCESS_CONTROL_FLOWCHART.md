# 🛡️ Unibook 접근통제 로직 순서도

## 📋 목차
1. [AuthorizationService 핵심 메서드](#authorizationservice-핵심-메서드)
2. [게시글 상세 조회 권한](#게시글-상세-조회-권한)
3. [게시글 수정 권한](#게시글-수정-권한)
4. [게시글 삭제 권한](#게시글-삭제-권한)
5. [게시글 상태 변경 권한](#게시글-상태-변경-권한)
6. [관리자 전용 기능](#관리자-전용-기능)

---

## AuthorizationService 핵심 메서드

### 🔍 isOwner() 메서드
```mermaid
flowchart TD
    A[isOwner 호출] --> B{post == null?}
    B -->|Yes| C[return false]
    B -->|No| D{userPrincipal == null?}
    D -->|Yes| C
    D -->|No| E{"post.getUser() == null?"}
    E -->|Yes| C
    E -->|No| F{"post.getUser().getUserId() == null?"}
    F -->|Yes| C
    F -->|No| G{"post.getUser().getUserId().equals<br/>userPrincipal.getUserId?"}
    G -->|Yes| H[return true]
    G -->|No| C
```

### 🔍 isAdmin() 메서드
```mermaid
flowchart TD
    A[isAdmin 호출] --> B{userPrincipal == null?}
    B -->|Yes| C[return false]
    B -->|No| D{권한에 ROLE_ADMIN 포함?}
    D -->|Yes| E[return true]
    D -->|No| C
```

### 🔍 canEdit() 메서드 (핵심 로직)
```mermaid
flowchart TD
    A[canEdit 호출] --> B{post == null OR<br/>userPrincipal == null?}
    B -->|Yes| C[return false]
    B -->|No| D[isOwner 확인]
    D --> E[isAdmin 확인]
    E --> F{isAdmin == true?}
    F -->|Yes| G[return true<br/>관리자는 모든 게시글 수정 가능]
    F -->|No| H{isOwner == true?}
    H -->|No| C
    H -->|Yes| I{"post.getStatus() != BLOCKED?"}
    I -->|Yes| J[return true<br/>소유자는 비BLOCKED 게시글만 수정 가능]
    I -->|No| C
```

---

## 게시글 상세 조회 권한

### 📖 GET /posts/{id} - 전체 흐름
```mermaid
flowchart TD
    A[게시글 상세 조회 요청] --> B[postService.getPostByIdWithDetails]
    B --> C{게시글 존재?}
    C -->|No| D[ResourceNotFoundException<br/>→ error/post-deleted]
    C -->|Yes| E{"post.getStatus() == BLOCKED?"}
    E -->|No| F[일반 게시글 처리]
    E -->|Yes| G[authorizationService.isOwnerOrAdmin 확인]
    G --> H{소유자 또는 관리자?}
    H -->|No| I[비인가 접근 로그<br/>model.addAttribute blocked=true<br/>→ error/post-deleted]
    H -->|Yes| J[BLOCKED 게시글 접근 허용]
    
    F --> K[조회수 증가 로직]
    J --> K
    K --> L[authorizationService.calculateDetailPageAuth]
    L --> M[권한 정보 계산<br/>isOwner, canEdit, isAdmin]
    M --> N[관련 게시글 조회]
    N --> O[찜 상태 확인]
    O --> P[model에 권한 정보 추가<br/>posts/detail 렌더링]
```

### 📖 calculateDetailPageAuth() 상세 로직
```mermaid
flowchart TD
    A[calculateDetailPageAuth 호출] --> B{userPrincipal == null?}
    B -->|Yes| C[isOwner=false<br/>isAdmin=false<br/>canEdit=false]
    B -->|No| D[isOwner 확인]
    D --> E[isAdmin 확인]
    E --> F[canEdit 확인]
    F --> G[AuthorizationInfo 객체 생성]
    G --> H[return AuthorizationInfo]
    C --> H
```

---

## 게시글 수정 권한

### ✏️ GET /posts/{id}/edit - 수정 폼 접근
```mermaid
flowchart TD
    A[수정 폼 접근] --> B{로그인 상태?}
    B -->|No| C["@PreAuthorize 차단<br/>401 Unauthorized"]
    B -->|Yes| D[postService.getPostByIdWithDetails]
    D --> E{게시글 존재?}
    E -->|No| F[ResourceNotFoundException]
    E -->|Yes| G[authorizationService.requireCanEdit]
    G --> H[canEdit 메서드 호출]
    H --> I{관리자?}
    I -->|Yes| J[수정 폼 표시]
    I -->|No| K{소유자?}
    K -->|No| L[AccessDeniedException<br/>게시글 수정 권한이 없습니다]
    K -->|Yes| M{BLOCKED 상태?}
    M -->|Yes| L
    M -->|No| J
```

### ✏️ POST /posts/{id}/edit - 수정 처리
```mermaid
flowchart TD
    A[수정 요청] --> B{로그인 상태?}
    B -->|No| C["@PreAuthorize 차단"]
    B -->|Yes| D[postService.getPostByIdWithDetails]
    D --> E{게시글 존재?}
    E -->|No| F[ResourceNotFoundException]
    E -->|Yes| G[authorizationService.requireCanEdit]
    G --> H[동일한 canEdit 로직]
    H --> I{권한 있음?}
    I -->|No| J[AccessDeniedException]
    I -->|Yes| K[이미지 개수 검증]
    K --> L{검증 통과?}
    L -->|No| M[bindingResult 에러<br/>폼 다시 표시]
    L -->|Yes| N[postService.updatePost 호출]
    N --> O[수정 완료<br/>상세 페이지로 리다이렉트]
```

---

## 게시글 삭제 권한

### 🗑️ POST /posts/{id}/delete
```mermaid
flowchart TD
    A[삭제 요청] --> B{로그인 상태?}
    B -->|No| C["@PreAuthorize 차단"]
    B -->|Yes| D[postService.getPostById]
    D --> E{게시글 존재?}
    E -->|No| F[ResourceNotFoundException]
    E -->|Yes| G[authorizationService.requireOwnerOrAdmin]
    G --> H[isOwnerOrAdmin 확인]
    H --> I{소유자 OR 관리자?}
    I -->|No| J[AccessDeniedException<br/>게시글 삭제 권한이 없습니다]
    I -->|Yes| K[postService.deletePost]
    K --> L[이미지 파일도 함께 삭제]
    L --> M[성공 메시지<br/>/posts로 리다이렉트]
```

### 🔍 requireOwnerOrAdmin() 상세 로직
```mermaid
flowchart TD
    A[requireOwnerOrAdmin 호출] --> B[isOwnerOrAdmin 확인]
    B --> C[isOwner 확인]
    C --> D[isAdmin 확인]
    D --> E{isOwner OR isAdmin?}
    E -->|Yes| F[권한 승인]
    E -->|No| G[AccessDeniedException 발생]
```

---

## 게시글 상태 변경 권한

### 🔄 POST /posts/{id}/status - 상태 변경 (AJAX)
```mermaid
flowchart TD
    A[상태 변경 요청] --> B{로그인 상태?}
    B -->|No| C["@PreAuthorize 차단"]
    B -->|Yes| D[postService.getPostById]
    D --> E{게시글 존재?}
    E -->|No| F[ResourceNotFoundException<br/>response: success=false]
    E -->|Yes| G[authorizationService.requireOwner]
    G --> H[isOwner 확인]
    H --> I{소유자?}
    I -->|No| J[AccessDeniedException<br/>게시글 상태 변경 권한이 없습니다]
    I -->|Yes| K{현재 상태가 BLOCKED?}
    K -->|Yes| L[ValidationException<br/>블라인드 처리된 게시글은<br/>상태 변경 불가]
    K -->|No| M[신고 수 확인]
    M --> N{신고자 3명 이상?}
    N -->|Yes| O[ValidationException<br/>다수 신고로 상태 변경 불가]
    N -->|No| P[postService.updatePostStatus]
    P --> Q[상태 변경 성공<br/>response: success=true]
```

### ⚠️ 중요한 제약 사항
```mermaid
flowchart TD
    A[상태 변경 요청] --> B{요청자 타입}
    B -->|소유자| C{현재 상태}
    B -->|관리자| D[❌ 관리자도 상태 변경 불가<br/>오직 소유자만 가능]
    B -->|타인| E[❌ 권한 없음]
    
    C -->|AVAILABLE| F[✅ 변경 가능]
    C -->|RESERVED| F
    C -->|COMPLETED| F
    C -->|BLOCKED| G[❌ BLOCKED 상태에서는<br/>상태 변경 불가]
```

---

## 관리자 전용 기능

### 🔒 PUT /posts/{id}/block - 게시글 차단
```mermaid
flowchart TD
    A[게시글 차단 요청] --> B{"@PreAuthorize<br/>hasRole ADMIN?"}
    B -->|No| C[403 Forbidden]
    B -->|Yes| D[postService.blockPost]
    D --> E{처리 성공?}
    E -->|No| F[Exception 발생<br/>response: success=false]
    E -->|Yes| G[차단 성공<br/>response: success=true]
```

### 🔓 PUT /posts/{id}/unblock - 게시글 차단 해제
```mermaid
flowchart TD
    A[차단 해제 요청] --> B{"@PreAuthorize<br/>hasRole ADMIN?"}
    B -->|No| C[403 Forbidden]
    B -->|Yes| D[postService.unblockPost]
    D --> E{처리 성공?}
    E -->|No| F[Exception 발생<br/>response: success=false]
    E -->|Yes| G[차단 해제 성공<br/>response: success=true]
```

---

## 🎯 핵심 보안 규칙 요약

### 📊 권한 매트릭스
| 기능 | 게시글 소유자 | 관리자 | 타인 | 비로그인 |
|------|---------------|--------|------|----------|
| **일반 게시글 조회** | ✅ | ✅ | ✅ | ✅ |
| **BLOCKED 게시글 조회** | ✅ | ✅ | ❌ | ❌ |
| **일반 게시글 수정** | ✅ | ✅ | ❌ | ❌ |
| **BLOCKED 게시글 수정** | ❌ | ✅ | ❌ | ❌ |
| **게시글 삭제** | ✅ | ✅ | ❌ | ❌ |
| **게시글 상태 변경** | ✅* | ❌ | ❌ | ❌ |
| **게시글 차단/해제** | ❌ | ✅ | ❌ | ❌ |

*\* BLOCKED 상태이거나 신고 3회 이상인 경우 불가*

### 🚨 중요한 보안 포인트

1. **BLOCKED 게시글 보안**:
   - 소유자도 수정 불가 (관리자만 가능)
   - 소유자도 상태 변경 불가
   - 비소유자는 조회도 불가

2. **상태 변경 제한**:
   - 관리자도 상태 변경 불가 (오직 소유자만)
   - BLOCKED 상태에서는 어떤 상태로도 변경 불가
   - 신고 3회 이상 시 상태 변경 차단

3. **Null 안전성**:
   - 모든 권한 체크에서 null 체크 수행
   - UserPrincipal, Post, User 모두 null 안전

4. **예외 처리**:
   - 권한 없음: `AccessDeniedException`
   - 리소스 없음: `ResourceNotFoundException`  
   - 검증 실패: `ValidationException`

이 순서도를 통해 Unibook의 모든 접근통제 로직을 한눈에 파악할 수 있습니다. 🛡️