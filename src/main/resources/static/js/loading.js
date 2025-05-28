// 로딩 상태 관리 유틸리티
const LoadingUtil = {
    // 전역 로딩 표시
    show: function(message = '처리 중입니다...') {
        const overlay = this.getOrCreateOverlay();
        overlay.querySelector('.loading-text').textContent = message;
        overlay.classList.add('show');
    },
    
    // 전역 로딩 숨김
    hide: function() {
        const overlay = document.getElementById('loadingOverlay');
        if (overlay) {
            overlay.classList.remove('show');
        }
    },
    
    // 버튼 로딩 상태
    showButtonLoading: function(button, loadingText = '처리 중...') {
        button.disabled = true;
        button.dataset.originalText = button.textContent;
        button.textContent = loadingText;
        button.classList.add('btn-loading');
    },
    
    hideButtonLoading: function(button) {
        button.disabled = false;
        button.textContent = button.dataset.originalText || '완료';
        button.classList.remove('btn-loading');
    },
    
    // 오버레이 생성
    getOrCreateOverlay: function() {
        let overlay = document.getElementById('loadingOverlay');
        if (!overlay) {
            overlay = document.createElement('div');
            overlay.id = 'loadingOverlay';
            overlay.className = 'loading-overlay';
            overlay.innerHTML = `
                <div class="spinner-wrapper">
                    <div class="spinner-border text-primary" role="status">
                        <span class="visually-hidden">Loading...</span>
                    </div>
                    <div class="loading-text">처리 중입니다...</div>
                </div>
            `;
            document.body.appendChild(overlay);
        }
        return overlay;
    }
};

// AJAX 요청 시 자동 로딩 표시 (global: false인 요청 제외)
if (typeof $ !== 'undefined') {
    $(document).ajaxStart(function() {
        LoadingUtil.show();
    }).ajaxStop(function() {
        LoadingUtil.hide();
    });
}

// 폼 제출 시 로딩 표시
document.addEventListener('DOMContentLoaded', function() {
    // 모든 폼에 대해
    document.querySelectorAll('form').forEach(form => {
        form.addEventListener('submit', function(e) {
            // 파일 업로드 폼이 아닌 경우
            if (!form.enctype || form.enctype !== 'multipart/form-data') {
                const submitButton = form.querySelector('button[type="submit"]');
                if (submitButton && !submitButton.disabled) {
                    LoadingUtil.showButtonLoading(submitButton);
                }
            }
        });
    });
});