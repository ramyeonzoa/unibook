/**
 * 검색 결과 하이라이팅 기능
 */
$(document).ready(function() {
    // 검색 키워드가 있을 때만 실행
    if (typeof searchKeywords !== 'undefined' && searchKeywords && searchKeywords.length > 0) {
        highlightSearchResults();
    }
});

/**
 * 검색 결과에서 키워드를 하이라이팅
 */
function highlightSearchResults() {
    // 하이라이팅할 요소들 선택
    const elementsToHighlight = [
        '.post-title',           // 게시글 제목
        '.post-description',     // 게시글 설명
        '.post-book-title',      // 책 제목
        '.post-book-author',     // 책 저자
        '.post-subject-name',    // 과목명
        '.post-professor-name'   // 교수명
    ];
    
    elementsToHighlight.forEach(selector => {
        $(selector).each(function() {
            let text = $(this).text();
            let highlightedText = highlightKeywords(text, searchKeywords);
            $(this).html(highlightedText);
        });
    });
}

/**
 * 텍스트에서 키워드를 찾아 하이라이팅
 * @param {string} text - 원본 텍스트
 * @param {array} keywords - 하이라이팅할 키워드 배열
 * @returns {string} - 하이라이팅된 HTML
 */
function highlightKeywords(text, keywords) {
    if (!text || !keywords || keywords.length === 0) {
        return text;
    }
    
    let highlightedText = text;
    
    // 각 키워드에 대해 처리
    keywords.forEach(keyword => {
        if (keyword && keyword.length >= 2) { // 2글자 이상만 하이라이팅
            // 대소문자 구분 없이 검색
            const regex = new RegExp('(' + escapeRegExp(keyword) + ')', 'gi');
            highlightedText = highlightedText.replace(regex, '<mark class="search-highlight">$1</mark>');
        }
    });
    
    return highlightedText;
}

/**
 * 정규식 특수문자 이스케이프
 * @param {string} string - 이스케이프할 문자열
 * @returns {string} - 이스케이프된 문자열
 */
function escapeRegExp(string) {
    return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/**
 * 하이라이팅 토글 기능 (선택사항)
 */
function toggleHighlight() {
    $('mark.search-highlight').toggleClass('highlight-off');
}