#!/usr/bin/env python3
"""
모든 FAQ에 keywords, variations, related_questions 추가
"""
import json
from pathlib import Path

# FAQ 강화 데이터
enhancements = {
    # 플랫폼 소개 (1개)
    "platform.why_unibook": {
        "keywords": ["장점", "이유", "특징", "안전", "학교", "이메일 인증"],
        "variations": [
            "유니북 왜 써요",
            "다른 중고거래 앱이랑 뭐가 달라요",
            "Unibook 특징",
            "왜 Unibook을 사용해야 하나요"
        ],
        "related_questions": ["platform.what_is_unibook", "guide.quick_start"]
    },

    # Guide 카테고리 (12개)
    "guide.quick_start": {
        "keywords": ["시작", "처음", "가입", "이용 방법", "튜토리얼"],
        "variations": [
            "처음 사용하는데 어떻게 해요",
            "Unibook 시작하기",
            "이용 방법",
            "어떻게 시작하나요"
        ],
        "related_questions": ["guide.signup_verification", "guide.search_basics"]
    },

    "guide.signup_verification": {
        "keywords": ["회원가입", "이메일", "인증", "가입 절차", "학교 이메일"],
        "variations": [
            "가입 어떻게 해요",
            "회원가입 방법",
            "학교 이메일로 가입",
            "인증 메일 안와요"
        ],
        "related_questions": ["faq.email_verification", "policy.email_verification_required"]
    },

    "guide.search_basics": {
        "keywords": ["검색", "교재 찾기", "도서 검색", "책 찾기"],
        "variations": [
            "책 어떻게 찾아요",
            "교재 검색 방법",
            "원하는 책 찾기",
            "도서 검색"
        ],
        "related_questions": ["guide.search_filters", "faq.find_books"]
    },

    "guide.wishlist": {
        "keywords": ["찜", "관심 목록", "즐겨찾기", "하트", "알림"],
        "variations": [
            "찜하기 기능",
            "하트 누르는 법",
            "관심 목록 저장",
            "즐겨찾기 사용법"
        ],
        "related_questions": ["faq.wishlist", "feature.wishlist_notifications"]
    },

    "guide.photo_tips": {
        "keywords": ["사진", "촬영", "이미지", "업로드", "대표 이미지"],
        "variations": [
            "사진 잘 찍는 법",
            "판매글 사진 팁",
            "어떤 사진을 올려야 하나요",
            "이미지 촬영 방법"
        ],
        "related_questions": ["guide.post_creation", "feature.post_image_requirements"]
    },

    "guide.status_management": {
        "keywords": ["거래 상태", "판매중", "예약중", "거래완료", "상태 변경"],
        "variations": [
            "상태 바꾸는 법",
            "판매중으로 변경",
            "예약중 처리",
            "거래완료 어떻게 해요"
        ],
        "related_questions": ["faq.status_management", "feature.chat_status_change"]
    },

    "guide.chat_start": {
        "keywords": ["채팅", "대화", "메시지", "판매자", "연락"],
        "variations": [
            "채팅 시작하는 법",
            "판매자한테 연락하기",
            "메시지 보내기",
            "대화 어떻게 시작해요"
        ],
        "related_questions": ["faq.start_chat", "faq.chat_images"]
    },

    "guide.keyword_alert": {
        "keywords": ["키워드", "알림", "등록", "실시간", "관심 키워드"],
        "variations": [
            "키워드 알림 설정",
            "관심 키워드 등록",
            "알림 받는 법",
            "키워드 등록 방법"
        ],
        "related_questions": ["faq.keyword_alert", "feature.keyword_alert_rules"]
    },

    "guide.dark_mode": {
        "keywords": ["다크 모드", "테마", "어두운 화면", "다크"],
        "variations": [
            "다크모드 설정",
            "어두운 테마",
            "화면 어둡게 하기",
            "야간 모드"
        ],
        "related_questions": ["faq.dark_mode"]
    },

    "guide.reporting": {
        "keywords": ["신고", "문제", "사기", "불법", "부적절"],
        "variations": [
            "신고하는 법",
            "문제 있는 게시글 신고",
            "사용자 신고",
            "불법 게시글 차단"
        ],
        "related_questions": ["faq.reporting", "policy.report_limits"]
    },

    "guide.my_page": {
        "keywords": ["마이페이지", "내 정보", "프로필", "설정", "관리"],
        "variations": [
            "마이페이지 기능",
            "내 정보 확인",
            "프로필 설정",
            "계정 관리"
        ],
        "related_questions": ["faq.wishlist", "faq.keyword_alert"]
    },

    "guide.contact": {
        "keywords": ["문의", "도움", "이메일", "고객센터", "지원"],
        "variations": [
            "문의하는 법",
            "고객센터 연락처",
            "도움이 필요해요",
            "어디로 연락해요"
        ],
        "related_questions": ["faq.contact"]
    },

    # FAQ 카테고리 (13개)
    "faq.signup_flow": {
        "keywords": ["회원가입", "가입 절차", "등록", "계정 생성"],
        "variations": [
            "가입 어떻게 해요",
            "회원가입 순서",
            "계정 만들기",
            "가입 절차"
        ],
        "related_questions": ["guide.signup_verification", "faq.email_verification"]
    },

    "faq.email_verification": {
        "keywords": ["이메일", "인증", "메일", "확인", "스팸"],
        "variations": [
            "인증 메일 안와요",
            "이메일 확인 안돼요",
            "인증 메일 재발송",
            "메일 안옴"
        ],
        "related_questions": ["guide.signup_verification", "policy.email_rate_limit"]
    },

    "faq.post_listing": {
        "keywords": ["게시글", "등록", "판매글", "작성"],
        "variations": [
            "판매글 올리는 법",
            "게시글 작성",
            "교재 등록하기",
            "판매 글쓰기"
        ],
        "related_questions": ["guide.post_creation", "guide.photo_tips"]
    },

    "faq.status_management": {
        "keywords": ["거래 상태", "상태 변경", "판매중", "예약중", "완료"],
        "variations": [
            "상태 어떻게 바꿔요",
            "거래 상태 관리",
            "판매중으로 변경",
            "상태 업데이트"
        ],
        "related_questions": ["guide.status_management"]
    },

    "faq.safe_trade": {
        "keywords": ["안전", "거래", "사기", "주의사항", "팁"],
        "variations": [
            "안전하게 거래하는 법",
            "사기 예방",
            "거래 주의사항",
            "안전 팁"
        ],
        "related_questions": ["guide.safe_meetup", "guide.parcel_trade"]
    },

    "faq.find_books": {
        "keywords": ["검색", "찾기", "도서", "교재", "빠르게"],
        "variations": [
            "책 빨리 찾기",
            "원하는 교재 검색",
            "도서 찾는 법",
            "효율적인 검색"
        ],
        "related_questions": ["guide.search_basics", "guide.search_filters"]
    },

    "faq.wishlist": {
        "keywords": ["찜", "하트", "관심", "저장", "목록"],
        "variations": [
            "찜하기",
            "하트 기능",
            "관심 목록",
            "저장하기"
        ],
        "related_questions": ["guide.wishlist", "feature.wishlist_notifications"]
    },

    "faq.keyword_alert": {
        "keywords": ["키워드", "알림", "실시간", "등록", "관심"],
        "variations": [
            "키워드 알림이 뭐에요",
            "관심 키워드",
            "실시간 알림",
            "키워드 등록"
        ],
        "related_questions": ["guide.keyword_alert", "feature.keyword_alert_rules"]
    },

    "faq.start_chat": {
        "keywords": ["채팅", "시작", "대화", "메시지", "연락"],
        "variations": [
            "채팅 시작",
            "메시지 보내기",
            "대화하기",
            "연락 시작"
        ],
        "related_questions": ["guide.chat_start", "faq.chat_images"]
    },

    "faq.chat_images": {
        "keywords": ["채팅", "이미지", "사진", "전송", "공유"],
        "variations": [
            "채팅에서 사진 보내기",
            "이미지 전송",
            "사진 공유",
            "이미지 첨부"
        ],
        "related_questions": ["guide.chat_start"]
    },

    "faq.dark_mode": {
        "keywords": ["다크모드", "테마", "화면", "설정", "어두운"],
        "variations": [
            "다크모드 켜기",
            "어두운 테마",
            "화면 설정",
            "야간 모드"
        ],
        "related_questions": ["guide.dark_mode"]
    },

    "faq.reporting": {
        "keywords": ["신고", "제재", "문제", "부적절", "차단"],
        "variations": [
            "신고하기",
            "문제 게시글 신고",
            "사용자 차단",
            "부적절한 콘텐츠 신고"
        ],
        "related_questions": ["guide.reporting", "policy.report_limits"]
    },

    "faq.contact": {
        "keywords": ["문의", "연락", "이메일", "도움", "지원"],
        "variations": [
            "문의하기",
            "연락처",
            "도움 받기",
            "고객 지원"
        ],
        "related_questions": ["guide.contact"]
    },

    # Feature 카테고리 (7개)
    "feature.keyword_alert_rules": {
        "keywords": ["키워드", "규칙", "제한", "10개", "최대"],
        "variations": [
            "키워드 몇 개까지",
            "키워드 제한",
            "최대 키워드 수",
            "키워드 규칙"
        ],
        "related_questions": ["faq.keyword_alert", "guide.keyword_alert"]
    },

    "feature.post_image_requirements": {
        "keywords": ["이미지", "사진", "업로드", "5장", "최대", "형식"],
        "variations": [
            "사진 몇 장까지",
            "이미지 업로드 제한",
            "사진 개수 제한",
            "이미지 요구사항"
        ],
        "related_questions": ["guide.photo_tips", "guide.post_creation"]
    },

    "feature.self_wishlist_restriction": {
        "keywords": ["찜", "내 게시글", "본인", "제한"],
        "variations": [
            "내 글도 찜 가능한가요",
            "본인 게시글 찜",
            "자기 글 찜하기"
        ],
        "related_questions": ["faq.wishlist"]
    },

    "feature.chat_leave_behavior": {
        "keywords": ["채팅방", "나가기", "퇴장", "삭제"],
        "variations": [
            "채팅방 나가면",
            "대화방 나가기",
            "채팅 종료",
            "채팅방 삭제"
        ],
        "related_questions": ["guide.chat_start"]
    },

    "feature.chat_status_change": {
        "keywords": ["채팅", "거래 상태", "변경", "알림"],
        "variations": [
            "채팅에서 상태 변경",
            "대화 중 상태 바꾸기",
            "채팅 내 상태 관리"
        ],
        "related_questions": ["guide.status_management", "guide.chat_start"]
    },

    "feature.wishlist_notifications": {
        "keywords": ["찜", "알림", "가격", "상태", "변경", "실시간"],
        "variations": [
            "찜한 책 가격 바뀌면",
            "찜 알림",
            "가격 변동 알림",
            "상태 변경 알림"
        ],
        "related_questions": ["faq.wishlist", "guide.wishlist"]
    },

    "feature.keyword_alert_flow": {
        "keywords": ["키워드", "알림", "처리", "순서", "흐름"],
        "variations": [
            "키워드 알림 작동 원리",
            "알림 처리 순서",
            "키워드 매칭 과정"
        ],
        "related_questions": ["faq.keyword_alert"]
    },

    # Policy 카테고리 (3개)
    "policy.email_verification_required": {
        "keywords": ["이메일 인증", "제한", "기능", "필수"],
        "variations": [
            "인증 안하면",
            "이메일 확인 안하면",
            "인증 필수인가요",
            "인증 전 기능"
        ],
        "related_questions": ["faq.email_verification", "guide.signup_verification"]
    },

    "policy.email_rate_limit": {
        "keywords": ["이메일", "제한", "60초", "5회", "쿨다운", "재발송"],
        "variations": [
            "메일 몇 번까지",
            "이메일 재발송 제한",
            "인증 메일 제한",
            "메일 쿨다운"
        ],
        "related_questions": ["faq.email_verification"]
    },

    "policy.report_limits": {
        "keywords": ["신고", "제한", "10건", "일일", "처리", "조치"],
        "variations": [
            "신고 몇 번까지",
            "하루에 신고 제한",
            "신고 횟수",
            "신고 후 조치"
        ],
        "related_questions": ["faq.reporting", "guide.reporting"]
    }
}

# JSON 파일 로드
faq_path = Path('src/main/resources/chatbot/rag_seed.json')
with open(faq_path, 'r', encoding='utf-8') as f:
    faqs = json.load(f)

# 강화 적용
enhanced_count = 0
for faq in faqs:
    faq_id = faq['id']
    if faq_id in enhancements:
        enhancement = enhancements[faq_id]
        faq['keywords'] = enhancement['keywords']
        faq['variations'] = enhancement['variations']
        faq['related_questions'] = enhancement['related_questions']
        enhanced_count += 1
        print(f"✅ {faq_id}")

# JSON 저장 (들여쓰기 2칸, ensure_ascii=False)
with open(faq_path, 'w', encoding='utf-8') as f:
    json.dump(faqs, f, ensure_ascii=False, indent=2)

print(f"\n총 {enhanced_count}개 FAQ 강화 완료!")
print(f"파일 저장: {faq_path}")
