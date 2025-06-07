/**
 * 다크모드 토글 기능
 * 모든 페이지에서 공통으로 사용
 */

// 다크모드 기능 초기화
document.addEventListener('DOMContentLoaded', function() {
    const savedTheme = localStorage.getItem('theme');
    const systemTheme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    const theme = savedTheme || systemTheme;
    
    // 아이콘 업데이트 함수
    function updateThemeIcons(theme) {
        const icons = ['themeIcon', 'themeIconMobile', 'themeIconGuest'];
        icons.forEach(iconId => {
            const icon = document.getElementById(iconId);
            if (icon) {
                icon.className = theme === 'dark' ? 'bi bi-moon-fill' : 'bi bi-sun-fill';
            }
        });
    }
    
    // 초기 아이콘 설정
    updateThemeIcons(theme);
    
    // 토글 함수 (애니메이션 활성화)
    function toggleTheme() {
        // 토글 시에만 애니메이션 활성화
        document.body.classList.add('theme-transitioning');
        
        const currentTheme = document.documentElement.getAttribute('data-bs-theme');
        const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
        
        document.documentElement.setAttribute('data-bs-theme', newTheme);
        localStorage.setItem('theme', newTheme);
        updateThemeIcons(newTheme);
        
        // 애니메이션 완료 후 클래스 제거
        setTimeout(() => {
            document.body.classList.remove('theme-transitioning');
        }, 200);
    }
    
    // 모든 토글 버튼에 이벤트 추가
    const themeToggle = document.getElementById('themeToggle');
    const themeToggleMobile = document.getElementById('themeToggleMobile');
    const themeToggleGuest = document.getElementById('themeToggleGuest');
    
    if (themeToggle) {
        themeToggle.addEventListener('click', toggleTheme);
    }
    if (themeToggleMobile) {
        themeToggleMobile.addEventListener('click', toggleTheme);
    }
    if (themeToggleGuest) {
        themeToggleGuest.addEventListener('click', toggleTheme);
    }
    
    // 전역 함수로 노출 (다른 스크립트에서 사용 가능)
    window.toggleTheme = toggleTheme;
    window.updateThemeIcons = updateThemeIcons;
});