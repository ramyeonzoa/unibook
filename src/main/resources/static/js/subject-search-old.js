$(document).ready(function() {
    let currentPage = 1;
    let currentQuery = '';
    let selectedSubject = null;
    let selectedProfessor = null;
    
    // CSRF 토큰 설정
    const token = $('meta[name="_csrf"]').attr('content');
    const header = $('meta[name="_csrf_header"]').attr('content');
    
    // AJAX 기본 설정에 CSRF 토큰 추가
    $.ajaxSetup({
        beforeSend: function(xhr) {
            if (header && token) {
                xhr.setRequestHeader(header, token);
            }
        }
    });
    
    // 과목 검색 버튼 클릭
    $('#subjectSearchBtn').on('click', function() {
        $('#subjectSearchModal').modal('show');
        resetModal();
        $('#professorSearchInput').focus();
    });
    
    // 모달 초기화
    function resetModal() {
        currentPage = 1;
        currentQuery = '';
        selectedSubject = null;
        selectedProfessor = null;
        
        $('#professorSearchInput').val('');
        $('#subjectSearchInput').val('').prop('disabled', true);
        $('#professorResults').empty();
        $('#subjectResults').empty();
        $('#selectedInfo').addClass('d-none');
        
        // 단계 표시 초기화
        $('#step1').removeClass('completed').addClass('active');
        $('#step2').removeClass('completed active').addClass('disabled');
        $('#step3').removeClass('completed active').addClass('disabled');
    }
    
    // === 1단계: 교수 검색 ===
    
    // 교수 검색 실행 버튼
    $('#professorSearchSubmit').on('click', function() {
        searchProfessors();
    });
    
    // 교수 검색 입력 필드에서 엔터키
    $('#professorSearchInput').on('keypress', function(e) {
        if (e.which === 13) {
            e.preventDefault();
            searchProfessors();
        }
    });
    
    // 교수 검색 함수
    function searchProfessors(page = 1) {
        const query = $('#professorSearchInput').val().trim();
        
        if (!query) {
            alert('교수명을 입력해주세요.');
            return;
        }
        
        currentQuery = query;
        currentPage = page;
        
        // 로딩 표시
        $('#professorResults').html('<div class="text-center p-3"><div class="spinner-border" role="status"></div></div>');
        
        $.get('/api/professors/search', {
            query: query,
            page: page,
            size: 10
        })
        .done(function(data) {
            displayProfessorResults(data);
        })
        .fail(function(xhr) {
            console.error('교수 검색 실패:', xhr);
            $('#professorResults').html('<div class="alert alert-danger">교수 검색 중 오류가 발생했습니다.</div>');
        });
    }
    
    // 교수 검색 결과 표시
    function displayProfessorResults(data) {
        const results = $('#professorResults');
        results.empty();
        
        if (!data.items || data.items.length === 0) {
            results.html('<div class="alert alert-info">검색 결과가 없습니다.</div>');
            return;
        }
        
        // 검색 결과 헤더
        results.append(`
            <div class="d-flex justify-content-between align-items-center mb-3">
                <h6 class="mb-0">교수 검색 결과 (총 ${data.total}건)</h6>
            </div>
        `);
        
        // 교수 목록
        const listGroup = $('<div class="list-group"></div>');
        
        data.items.forEach(function(professor) {
            const professorItem = $(`
                <button type="button" class="list-group-item list-group-item-action professor-item" 
                        data-professor-id="${professor.professorId}">
                    <div class="d-flex w-100 justify-content-between">
                        <h6 class="mb-1">${professor.professorName}</h6>
                        <small class="text-muted">${professor.departmentName}</small>
                    </div>
                </button>
            `);
            
            professorItem.on('click', function() {
                selectProfessor(professor);
            });
            
            listGroup.append(professorItem);
        });
        
        results.append(listGroup);
        
        // 페이징
        if (data.totalPages > 1) {
            results.append(createPagination(data, 'searchProfessors'));
        }
    }
    
    // 교수 선택
    function selectProfessor(professor) {
        selectedProfessor = professor;
        
        // UI 업데이트
        $('.professor-item').removeClass('active');
        $(`.professor-item[data-professor-id="${professor.professorId}"]`).addClass('active');
        
        // 2단계 활성화
        $('#step1').removeClass('active').addClass('completed');
        $('#step2').removeClass('disabled').addClass('active');
        $('#subjectSearchInput').prop('disabled', false).focus();
        
        // 과목 검색 결과 초기화
        $('#subjectResults').empty();
        selectedSubject = null;
        $('#step3').removeClass('completed active').addClass('disabled');
        $('#selectedInfo').addClass('d-none');
        
        // 선택된 교수 정보 표시
        $('#selectedProfessorInfo').html(`
            <div class="alert alert-success">
                <strong>${professor.professorName}</strong> 교수 (${professor.departmentName}) 선택됨
            </div>
        `);
    }
    
    // === 2단계: 과목 검색 ===
    
    // 과목 검색 실행 버튼
    $('#subjectSearchSubmit').on('click', function() {
        searchSubjects();
    });
    
    // 과목 검색 입력 필드에서 엔터키
    $('#subjectSearchInput').on('keypress', function(e) {
        if (e.which === 13) {
            e.preventDefault();
            searchSubjects();
        }
    });
    
    // 과목 검색 함수
    function searchSubjects(page = 1) {
        if (!selectedProfessor) {
            alert('먼저 교수를 선택해주세요.');
            return;
        }
        
        const query = $('#subjectSearchInput').val().trim();
        
        if (!query) {
            alert('과목명을 입력해주세요.');
            return;
        }
        
        // 로딩 표시
        $('#subjectResults').html('<div class="text-center p-3"><div class="spinner-border" role="status"></div></div>');
        
        $.get('/api/subjects/search', {
            query: query,
            professorId: selectedProfessor.professorId,
            page: page,
            size: 10
        })
        .done(function(data) {
            displaySubjectResults(data);
        })
        .fail(function(xhr) {
            console.error('과목 검색 실패:', xhr);
            $('#subjectResults').html('<div class="alert alert-danger">과목 검색 중 오류가 발생했습니다.</div>');
        });
    }
    
    // 과목 검색 결과 표시
    function displaySubjectResults(data) {
        const results = $('#subjectResults');
        results.empty();
        
        if (!data.items || data.items.length === 0) {
            // 검색 결과가 없으면 새 과목 생성 옵션 제공
            results.html(`
                <div class="alert alert-info">
                    검색 결과가 없습니다. 새 과목을 생성하시겠습니까?
                </div>
                <button type="button" class="btn btn-primary" id="createNewSubjectBtn">
                    "${$('#subjectSearchInput').val()}" 과목 생성
                </button>
            `);
            
            $('#createNewSubjectBtn').on('click', function() {
                createNewSubject($('#subjectSearchInput').val());
            });
            return;
        }
        
        // 검색 결과 헤더
        results.append(`
            <div class="d-flex justify-content-between align-items-center mb-3">
                <h6 class="mb-0">과목 검색 결과 (총 ${data.total}건)</h6>
                <button type="button" class="btn btn-sm btn-outline-primary" id="createNewSubjectBtn2">
                    새 과목 생성
                </button>
            </div>
        `);
        
        $('#createNewSubjectBtn2').on('click', function() {
            createNewSubject($('#subjectSearchInput').val());
        });
        
        // 과목 목록
        const listGroup = $('<div class="list-group"></div>');
        
        data.items.forEach(function(subject) {
            const subjectItem = $(`
                <button type="button" class="list-group-item list-group-item-action subject-item" 
                        data-subject-id="${subject.subjectId}">
                    <div class="d-flex w-100 justify-content-between">
                        <h6 class="mb-1">${subject.subjectName}</h6>
                        <small class="text-muted">${subject.subjectType === 'MAJOR' ? '전공' : '교양'}</small>
                    </div>
                    <p class="mb-1 text-muted">${subject.professorName} 교수</p>
                </button>
            `);
            
            subjectItem.on('click', function() {
                selectSubject(subject);
            });
            
            listGroup.append(subjectItem);
        });
        
        results.append(listGroup);
        
        // 페이징
        if (data.totalPages > 1) {
            results.append(createPagination(data, 'searchSubjects'));
        }
    }
    
    // 새 과목 생성
    function createNewSubject(subjectName) {
        if (!selectedProfessor) {
            alert('교수가 선택되지 않았습니다.');
            return;
        }
        
        if (!subjectName || subjectName.trim() === '') {
            alert('과목명을 입력해주세요.');
            return;
        }
        
        const requestData = {
            subjectName: subjectName.trim(),
            professorId: selectedProfessor.professorId,
            subjectType: 'MAJOR' // 기본값
        };
        
        $.post('/api/subjects/select', JSON.stringify(requestData), function(response) {
            if (response.success !== false) { // API는 성공 시 subject 객체 반환
                selectSubject(response);
            } else {
                alert('과목 생성에 실패했습니다: ' + (response.message || '알 수 없는 오류'));
            }
        }, 'json')
        .fail(function(xhr) {
            console.error('과목 생성 실패:', xhr);
            alert('과목 생성 중 오류가 발생했습니다.');
        });
    }
    
    // 과목 선택
    function selectSubject(subject) {
        selectedSubject = subject;
        
        // UI 업데이트
        $('.subject-item').removeClass('active');
        if (subject.subjectId) {
            $(`.subject-item[data-subject-id="${subject.subjectId}"]`).addClass('active');
        }
        
        // 3단계 활성화
        $('#step2').removeClass('active').addClass('completed');
        $('#step3').removeClass('disabled').addClass('active completed');
        
        // 선택 완료 정보 표시
        updateSelectedInfo();
    }
    
    // 선택 완료 정보 업데이트
    function updateSelectedInfo() {
        if (!selectedProfessor || !selectedSubject) return;
        
        $('#selectedInfo').removeClass('d-none');
        $('#selectedSubjectInfo').html(`
            <div class="card">
                <div class="card-body">
                    <h6 class="card-title">${selectedSubject.subjectName}</h6>
                    <p class="card-text">
                        <strong>교수:</strong> ${selectedProfessor.professorName}<br>
                        <strong>학과:</strong> ${selectedProfessor.departmentName}<br>
                        <strong>타입:</strong> ${selectedSubject.subjectType === 'MAJOR' ? '전공' : '교양'}
                    </p>
                </div>
            </div>
        `);
    }
    
    // === 확인 및 모달 닫기 ===
    
    // 과목 선택 확인
    $('#confirmSubjectSelection').on('click', function() {
        if (!selectedSubject) {
            alert('과목을 선택해주세요.');
            return;
        }
        
        // 메인 폼에 선택된 과목 정보 설정
        $('#subjectId').val(selectedSubject.subjectId);
        $('#selectedSubjectDisplay').html(`
            <div class="alert alert-info">
                <strong>선택된 과목:</strong> ${selectedSubject.subjectName} 
                (${selectedProfessor.professorName} 교수)
                <button type="button" class="btn btn-sm btn-outline-secondary ms-2" id="removeSubjectBtn">
                    제거
                </button>
            </div>
        `);
        
        // 제거 버튼 이벤트
        $('#removeSubjectBtn').on('click', function() {
            $('#subjectId').val('');
            $('#selectedSubjectDisplay').empty();
        });
        
        // 모달 닫기
        $('#subjectSearchModal').modal('hide');
    });
    
    // === 유틸리티 함수 ===
    
    // 페이징 생성
    function createPagination(data, searchFunction) {
        if (data.totalPages <= 1) return '';
        
        let pagination = '<nav aria-label="검색 결과 페이징"><ul class="pagination pagination-sm justify-content-center mt-3">';
        
        // 이전 페이지
        if (data.hasPrevious) {
            pagination += `<li class="page-item">
                <button class="page-link" onclick="${searchFunction}(${data.page - 1})">이전</button>
            </li>`;
        }
        
        // 페이지 번호 (현재 페이지 주변 5개만 표시)
        const startPage = Math.max(1, data.page - 2);
        const endPage = Math.min(data.totalPages, data.page + 2);
        
        for (let i = startPage; i <= endPage; i++) {
            const activeClass = i === data.page ? 'active' : '';
            pagination += `<li class="page-item ${activeClass}">
                <button class="page-link" onclick="${searchFunction}(${i})">${i}</button>
            </li>`;
        }
        
        // 다음 페이지
        if (data.hasNext) {
            pagination += `<li class="page-item">
                <button class="page-link" onclick="${searchFunction}(${data.page + 1})">다음</button>
            </li>`;
        }
        
        pagination += '</ul></nav>';
        return pagination;
    }
});