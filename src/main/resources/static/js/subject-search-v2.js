$(document).ready(function() {
    let currentPage = 1;
    let currentQuery = '';
    let selectedSubject = null;
    let currentUser = null; // 현재 사용자 정보
    let departments = []; // 학과 목록
    
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
        loadUserInfoAndDepartments();
        $('#subjectSearchInput').focus();
    });
    
    // 모달 초기화
    function resetModal() {
        currentPage = 1;
        currentQuery = '';
        selectedSubject = null;
        
        $('#subjectSearchInput').val('');
        $('#subjectResults').empty();
        $('#selectedInfo').addClass('d-none');
        $('#createNewSubjectSection').addClass('d-none');
        
        // 새 과목 생성 폼 초기화
        $('#newSubjectName').val('');
        $('#newProfessorName').val('');
        $('#newSubjectType').val(''); // 기본값 제거
        $('#newDepartmentId').val('');
        
        // 수강 시기 입력 폼 초기화
        $('#subjectTakenYearSection').addClass('d-none');
        $('#subjectTakenYear').val('');
        $('#subjectTakenSemester').val('');
        
        // 버튼 비활성화
        $('#createNewSubjectBtn').prop('disabled', true);
    }
    
    // === 사용자 정보 및 학과 목록 로드 ===
    
    // 사용자 정보와 학과 목록을 동시에 로드
    function loadUserInfoAndDepartments() {
        // 현재 사용자 정보 로드
        $.get('/api/users/me')
            .done(function(user) {
                currentUser = user;
                
                // 학과 목록 로드
                if (user.school && user.school.schoolId) {
                    loadDepartments(user.school.schoolId);
                } else {
                    console.error('사용자의 학교 정보가 없습니다.');
                    alert('사용자의 학교 정보가 없습니다. 관리자에게 문의하세요.');
                }
            })
            .fail(function(xhr) {
                console.error('사용자 정보 로드 실패:', xhr);
                alert('사용자 정보를 불러올 수 없습니다.');
            });
    }
    
    // 학과 목록 로드
    function loadDepartments(schoolId) {
        $.get('/api/departments/by-school/' + schoolId)
            .done(function(data) {
                departments = data;
                updateDepartmentSelect();
            })
            .fail(function(xhr) {
                console.error('학과 목록 로드 실패:', xhr);
                alert('학과 목록을 불러올 수 없습니다.');
            });
    }
    
    // 학과 선택 드롭다운 업데이트
    function updateDepartmentSelect() {
        const select = $('#newDepartmentId');
        select.empty();
        
        // 기본 옵션
        select.append('<option value="">학과를 선택하세요</option>');
        
        // 내 학과 우선 표시 (실제 departmentId 값으로 설정)
        if (currentUser && currentUser.department) {
            select.append(`<option value="${currentUser.department.departmentId}">${currentUser.department.departmentName} (내 학과)</option>`);
        }
        
        // 다른 학과들
        departments.forEach(function(dept) {
            if (!currentUser.department || dept.id !== currentUser.department.departmentId) {
                select.append(`<option value="${dept.id}">${dept.name}</option>`);
            }
        });
    }
    
    // === 과목명 우선 검색 ===
    
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
    
    // 과목 검색 함수 (학교 내에서만)
    function searchSubjects(page = 1) {
        const query = $('#subjectSearchInput').val().trim();
        
        if (!query || query.length < 2) {
            alert('과목명을 2자 이상 입력해주세요.');
            return;
        }
        
        currentQuery = query;
        currentPage = page;
        
        // 로딩 표시
        $('#subjectResults').html('<div class="text-center p-3"><div class="spinner-border" role="status"></div></div>');
        
        // 사용자의 학교 내에서만 검색
        $.get('/api/subjects/search/my-school', {
            query: query,
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
            // 검색 결과가 없으면 새 과목 생성 섹션 표시
            results.html(`
                <div class="alert alert-info">
                    <i class="bi bi-info-circle"></i> 검색 결과가 없습니다.
                    아래에서 새 과목을 생성할 수 있습니다.
                </div>
            `);
            
            // 새 과목 생성 섹션 표시
            showCreateNewSubjectSection();
            return;
        }
        
        // 검색 결과 헤더
        results.append(`
            <div class="d-flex justify-content-between align-items-center mb-3">
                <h6 class="mb-0">과목 검색 결과 (총 ${data.total}건)</h6>
                <button type="button" class="btn btn-sm btn-outline-primary" id="showCreateFormBtn">
                    <i class="bi bi-plus-circle"></i> 새 과목 생성
                </button>
            </div>
        `);
        
        // 새 과목 생성 버튼 이벤트
        $('#showCreateFormBtn').on('click', function() {
            showCreateNewSubjectSection();
        });
        
        // 과목 목록
        const listGroup = $('<div class="list-group"></div>');
        
        data.items.forEach(function(subject) {
            const subjectItem = $(`
                <button type="button" class="list-group-item list-group-item-action subject-item" 
                        data-subject-id="${subject.subjectId}">
                    <div class="d-flex w-100 justify-content-between">
                        <div>
                            <h6 class="mb-1">${subject.subjectName}</h6>
                            <p class="mb-0 text-muted">
                                <i class="bi bi-person"></i> ${subject.professorName} 교수
                                <span class="mx-2">•</span>
                                <i class="bi bi-building-fill"></i> ${subject.departmentName}
                            </p>
                        </div>
                        <div>
                            <span class="badge ${subject.type === 'MAJOR' ? 'bg-primary' : 'bg-info'}">
                                ${subject.type === 'MAJOR' ? '전공' : '교양'}
                            </span>
                        </div>
                    </div>
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
            results.append(createPagination(data, searchSubjects));
        }
    }
    
    // 새 과목 생성 섹션 표시
    function showCreateNewSubjectSection() {
        $('#createNewSubjectSection').removeClass('d-none');
        $('#newSubjectName').val(currentQuery); // 검색어를 과목명으로 자동 입력
        
        // 폼 초기화 (모든 필드를 사용자가 직접 선택하도록)
        $('#newSubjectType').val('');
        $('#newDepartmentId').val('');
        $('#departmentSelectGroup').removeClass('d-none'); // 항상 표시 (교양과목도 학과 선택 가능)
        
        // 버튼 비활성화 상태로 시작
        $('#createNewSubjectBtn').prop('disabled', true);
        
        // 필드 검증 이벤트 추가
        validateSubjectForm();
        
        $('#newProfessorName').focus();
    }
    
    // 필드 검증 함수
    function validateSubjectForm() {
        // 모든 필수 필드에 대한 이벤트 리스너 추가
        $('#newSubjectName, #newProfessorName, #newSubjectType, #newDepartmentId').off('input change').on('input change', function() {
            checkFormValidity();
        });
    }
    
    // 폼 유효성 검사 및 버튼 활성화/비활성화
    function checkFormValidity() {
        const subjectName = $('#newSubjectName').val().trim();
        const professorName = $('#newProfessorName').val().trim();
        const subjectType = $('#newSubjectType').val();
        const departmentId = $('#newDepartmentId').val();
        
        // 기본 필수 필드 검증
        let isValid = subjectName && professorName && subjectType;
        
        // 전공과목인 경우에만 학과 선택 필수
        if (subjectType === 'MAJOR') {
            isValid = isValid && departmentId;
        }
        // 교양과목은 학과 선택이 선택사항 (nullable)
        
        $('#createNewSubjectBtn').prop('disabled', !isValid);
    }
    
    // 과목 타입 변경 시 학과 선택 토글
    $('#newSubjectType').on('change', function() {
        const selectedType = $(this).val();
        if (selectedType === 'GENERAL') {
            $('#departmentSelectGroup').addClass('d-none');
            $('#newDepartmentId').val(''); // 교양과목은 학과 선택 초기화
        } else if (selectedType === 'MAJOR') {
            $('#departmentSelectGroup').removeClass('d-none');
        } else {
            $('#departmentSelectGroup').removeClass('d-none');
        }
        checkFormValidity(); // 타입 변경 시 유효성 재검사
    });
    
    // 새 과목 생성 버튼
    $('#createNewSubjectBtn').on('click', function() {
        createNewSubject();
    });
    
    // 새 과목 생성
    function createNewSubject() {
        const subjectName = $('#newSubjectName').val().trim();
        const professorName = $('#newProfessorName').val().trim();
        const subjectType = $('#newSubjectType').val();
        const departmentId = $('#newDepartmentId').val();
        
        // 검증 (기본적으로 버튼이 비활성화되어 있지만 추가 검증)
        if (!subjectName) {
            alert('과목명을 입력해주세요.');
            $('#newSubjectName').focus();
            return;
        }
        
        if (!professorName) {
            alert('교수명을 입력해주세요.');
            $('#newProfessorName').focus();
            return;
        }
        
        if (!subjectType) {
            alert('과목 타입을 선택해주세요.');
            $('#newSubjectType').focus();
            return;
        }
        
        // 전공과목일 때만 학과 검증 (교양과목은 자동으로 교양학부 배정됨)
        if (subjectType === 'MAJOR' && !departmentId) {
            alert('전공과목은 학과를 선택해야 합니다.');
            $('#newDepartmentId').focus();
            return;
        }
        
        // 로딩 표시
        const $btn = $('#createNewSubjectBtn');
        const originalText = $btn.html();
        $btn.prop('disabled', true).html('<span class="spinner-border spinner-border-sm"></span> 생성 중...');
        
        const requestData = {
            subjectName: subjectName,
            professorName: professorName,
            subjectType: subjectType,
            departmentId: subjectType === 'GENERAL' ? null : departmentId
        };
        
        $.ajax({
            url: '/api/subjects/create-with-professor',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(requestData),
            success: function(response) {
                // 생성 성공
                selectSubject(response);
                
                // 생성 섹션 숨기기
                $('#createNewSubjectSection').addClass('d-none');
                
                // 성공 메시지
                const alertHtml = `
                    <div class="alert alert-success alert-dismissible fade show" role="alert">
                        <i class="bi bi-check-circle"></i> 과목이 성공적으로 생성되었습니다.
                        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                    </div>
                `;
                $('#subjectResults').prepend(alertHtml);
            },
            error: function(xhr) {
                console.error('과목 생성 실패:', xhr);
                let errorMsg = '과목 생성 중 오류가 발생했습니다.';
                
                if (xhr.responseJSON && xhr.responseJSON.message) {
                    errorMsg = xhr.responseJSON.message;
                }
                
                alert(errorMsg);
            },
            complete: function() {
                // 버튼 복원
                $btn.prop('disabled', false).html(originalText);
            }
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
        
        // 수강 시기 입력 섹션 표시
        showSubjectTakenYearSection();
        
        // 선택 완료 정보 표시
        updateSelectedInfo();
    }
    
    // 수강 시기 입력 섹션 표시
    function showSubjectTakenYearSection() {
        $('#subjectTakenYearSection').removeClass('d-none');
        
        // 연도 옵션 동적 생성 (현재 연도 기준 5년 전까지)
        const now = new Date();
        const currentYear = now.getFullYear();
        const yearSelect = $('#subjectTakenYear');
        yearSelect.empty().append('<option value="">연도를 선택하세요</option>');
        
        for (let year = currentYear - 5; year <= currentYear; year++) {
            yearSelect.append(`<option value="${year}">${year}년</option>`);
        }
        
        // 현재 학기 추정 (3~8월: 1학기, 9~2월: 2학기)
        const currentMonth = now.getMonth() + 1; // 0-based이므로 +1
        const estimatedSemester = (currentMonth >= 3 && currentMonth <= 8) ? 'SPRING' : 'FALL';
        
        // 기본값 설정
        $('#subjectTakenYear').val(currentYear);
        $('#subjectTakenSemester').val(estimatedSemester);
    }
    
    // 선택 완료 정보 업데이트
    function updateSelectedInfo() {
        if (!selectedSubject) return;
        
        $('#selectedInfo').removeClass('d-none');
        $('#selectedSubjectInfo').html(`
            <div class="card border-success">
                <div class="card-header bg-success text-white">
                    <i class="bi bi-check-circle"></i> 선택된 과목
                </div>
                <div class="card-body">
                    <h6 class="card-title">${selectedSubject.subjectName}</h6>
                    <p class="card-text mb-0">
                        <strong>교수:</strong> ${selectedSubject.professorName}<br>
                        <strong>학과:</strong> ${selectedSubject.departmentName}<br>
                        <strong>타입:</strong> 
                        <span class="badge ${selectedSubject.type === 'MAJOR' ? 'bg-primary' : 'bg-info'}">
                            ${selectedSubject.type === 'MAJOR' ? '전공' : '교양'}
                        </span>
                    </p>
                </div>
            </div>
        `);
        
        // 확인 버튼 활성화
        $('#confirmSubjectSelection').prop('disabled', false);
    }
    
    // === 확인 및 모달 닫기 ===
    
    // 과목 선택 확인
    $('#confirmSubjectSelection').on('click', function() {
        if (!selectedSubject) {
            alert('과목을 선택해주세요.');
            return;
        }
        
        // 수강 시기 정보 검증
        const takenYear = $('#subjectTakenYear').val();
        const takenSemester = $('#subjectTakenSemester').val();
        
        if (!takenYear || !takenSemester) {
            alert('수강 연도와 학기를 선택해주세요.');
            return;
        }
        
        // 메인 폼에 선택된 과목 정보 및 수강 시기 설정
        $('#subjectId').val(selectedSubject.subjectId);
        $('#takenYear').val(takenYear);
        $('#takenSemester').val(takenSemester);
        $('#removeSubject').val('false'); // 과목 연결 해제 플래그 초기화
        
        // 학기 표시명 변환
        const semesterText = takenSemester === 'SPRING' ? '1학기' : 
                            takenSemester === 'FALL' ? '2학기' : 
                            takenSemester === 'SUMMER' ? '여름학기' : 
                            takenSemester === 'WINTER' ? '겨울학기' : '';
        
        $('#selectedSubjectDisplay').html(`
            <div class="alert alert-info d-flex justify-content-between align-items-center">
                <div>
                    <i class="bi bi-book"></i> <strong>${selectedSubject.subjectName}</strong>
                    <span class="text-muted ms-2">${selectedSubject.professorName} 교수</span>
                    <span class="text-muted ms-2">${takenYear}년 ${semesterText}</span>
                </div>
                <button type="button" class="btn btn-sm btn-outline-danger" id="removeSubjectBtn">
                    <i class="bi bi-x-circle"></i> 선택 취소
                </button>
            </div>
        `);
        
        // 과목 선택 완료 후 버튼 숨기기 (교재 선택과 동일한 패턴)
        $('#subjectSearchBtnDiv').hide();
        
        // 제거 버튼 이벤트
        $('#removeSubjectBtn').on('click', function() {
            $('#subjectId').val('');
            $('#takenYear').val('');
            $('#takenSemester').val('');
            $('#removeSubject').val('true'); // 수정 시 과목 연결 해제를 명시적으로 표시
            $('#selectedSubjectDisplay').empty();
            $('#subjectSearchBtnDiv').show(); // 과목 선택 버튼 다시 표시
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
                <a class="page-link" href="#" data-page="${data.page - 1}">이전</a>
            </li>`;
        }
        
        // 페이지 번호 (현재 페이지 주변 5개만 표시)
        const startPage = Math.max(1, data.page - 2);
        const endPage = Math.min(data.totalPages, data.page + 2);
        
        for (let i = startPage; i <= endPage; i++) {
            const activeClass = i === data.page ? 'active' : '';
            pagination += `<li class="page-item ${activeClass}">
                <a class="page-link" href="#" data-page="${i}">${i}</a>
            </li>`;
        }
        
        // 다음 페이지
        if (data.hasNext) {
            pagination += `<li class="page-item">
                <a class="page-link" href="#" data-page="${data.page + 1}">다음</a>
            </li>`;
        }
        
        pagination += '</ul></nav>';
        
        // jQuery 객체로 변환하고 이벤트 바인딩
        const $pagination = $(pagination);
        $pagination.find('.page-link').on('click', function(e) {
            e.preventDefault();
            const page = $(this).data('page');
            searchFunction(page);
        });
        
        return $pagination;
    }
});