/**
 * 이메일 인증 재발송 기능
 * 모든 페이지에서 사용 가능한 전역 스크립트
 */

$(document).ready(function() {
    // 인증 메일 재발송 버튼 이벤트 (헤더의 버튼)
    $('#resendVerificationBtn').on('click', function() {
        handleEmailResend($(this));
    });
    
    // 인증 페이지의 재발송 버튼 이벤트 (verification-required.html)
    $('#resendBtn').on('click', function() {
        handleEmailResend($(this));
    });
    
    /**
     * 이메일 재발송 처리 공통 함수
     */
    function handleEmailResend($button) {
        // 버튼 비활성화 및 로딩 표시
        const originalText = $button.html();
        $button.prop('disabled', true).html('<i class="bi bi-hourglass-split"></i> 발송 중...');
        
        // CSRF 토큰 가져오기
        const token = $('meta[name="_csrf"]').attr('content');
        const header = $('meta[name="_csrf_header"]').attr('content');
        
        $.ajax({
            url: '/api/auth/resend-verification',
            type: 'POST',
            beforeSend: function(xhr) {
                if (header && token) {
                    xhr.setRequestHeader(header, token);
                }
            },
            success: function(response) {
                showMessage('success', response.message || '인증 메일이 재발송되었습니다.');
                
                // 버튼을 30초간 비활성화 (Rate Limiting 고려)
                let countdown = 30;
                const interval = setInterval(function() {
                    $button.html(`<i class="bi bi-clock"></i> ${countdown}초 후 재시도`);
                    countdown--;
                    
                    if (countdown < 0) {
                        clearInterval(interval);
                        $button.prop('disabled', false).html(originalText);
                    }
                }, 1000);
            },
            error: function(xhr, status, error) {
                let errorMessage = '인증 메일 재발송 중 오류가 발생했습니다.';
                
                if (xhr.responseJSON && xhr.responseJSON.message) {
                    errorMessage = xhr.responseJSON.message;
                } else if (xhr.status === 429) {
                    errorMessage = '잠시 후 다시 시도해주세요. (1분 간격으로 재발송 가능)';
                } else if (xhr.status === 400) {
                    errorMessage = '이미 인증이 완료된 계정입니다.';
                }
                
                showMessage('error', errorMessage);
                
                // 버튼 복구
                $button.prop('disabled', false).html(originalText);
            }
        });
    }
    
    /**
     * 메시지 표시 함수
     */
    function showMessage(type, message) {
        // 기존 메시지 제거
        $('.resend-message').remove();
        
        const alertClass = type === 'success' ? 'alert-success' : 'alert-danger';
        const iconClass = type === 'success' ? 'bi-check-circle-fill' : 'bi-exclamation-circle-fill';
        
        const messageHtml = `
            <div class="alert ${alertClass} alert-dismissible fade show resend-message mt-3" role="alert">
                <i class="${iconClass} me-2"></i>
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        `;
        
        // 메시지를 적절한 위치에 표시
        if ($('#verificationAlert').length > 0) {
            // 헤더의 인증 알림 다음에 표시
            $('#verificationAlert').after(messageHtml);
        } else if ($('.container').length > 0) {
            // 페이지 컨테이너 상단에 표시
            $('.container').first().prepend(messageHtml);
        } else {
            // 기본적으로 body 상단에 표시
            $('body').prepend('<div class="container">' + messageHtml + '</div>');
        }
        
        // 성공 메시지는 5초 후 자동 숨김
        if (type === 'success') {
            setTimeout(function() {
                $('.resend-message').fadeOut(500, function() {
                    $(this).remove();
                });
            }, 5000);
        }
    }
});