$(document).ready(function() {
    let currentPage = 1;
    let currentQuery = '';
    let selectedBook = null;
    
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
    
    // 책 검색 버튼 클릭
    $('#bookSearchBtn').on('click', function() {
        $('#bookSearchModal').modal('show');
        $('#bookSearchInput').focus();
    });
    
    // 검색 실행 버튼 클릭
    $('#bookSearchSubmit').on('click', function() {
        searchBooks();
    });
    
    // 엔터키로 검색
    $('#bookSearchInput').on('keypress', function(e) {
        if (e.which === 13) {
            e.preventDefault();
            searchBooks();
        }
    });
    
    // 자동 검색 제거 - 명시적 검색만 수행
    
    // 책 검색 함수
    function searchBooks(page = 1) {
        const query = $('#bookSearchInput').val().trim();
        
        if (!query) {
            alert('검색어를 입력해주세요.');
            return;
        }
        
        currentQuery = query;
        currentPage = page;
        
        // UI 상태 변경
        $('#bookSearchResults').hide();
        $('#noBookResults').hide();
        $('#bookSearchLoading').show();
        
        // AJAX 요청
        $.ajax({
            url: '/api/books/search',
            method: 'GET',
            data: {
                query: query,
                page: page,
                size: 10
            },
            success: function(response) {
                $('#bookSearchLoading').hide();
                
                if (response.items && response.items.length > 0) {
                    displaySearchResults(response);
                } else {
                    $('#noBookResults').show();
                }
            },
            error: function(xhr, status, error) {
                $('#bookSearchLoading').hide();
                
                let errorMessage = '책 검색 중 오류가 발생했습니다.';
                if (xhr.responseJSON && xhr.responseJSON.message) {
                    errorMessage = xhr.responseJSON.message;
                }
                
                alert(errorMessage);
            }
        });
    }
    
    // 검색 결과 표시
    function displaySearchResults(response) {
        const container = $('#bookResultsContainer');
        container.empty();
        
        response.items.forEach(function(book) {
            const bookCard = createBookCard(book);
            container.append(bookCard);
        });
        
        // 페이지네이션 처리
        updatePagination(response.total, currentPage);
        
        $('#bookSearchResults').show();
    }
    
    // 책 카드 생성
    function createBookCard(book) {
        // HTML 태그 제거
        const title = book.title.replace(/<[^>]*>/g, '');
        const author = book.author.replace(/<[^>]*>/g, '');
        const publisher = book.publisher.replace(/<[^>]*>/g, '');
        const description = book.description ? book.description.replace(/<[^>]*>/g, '') : '';
        
        const card = $(`
            <div class="col-12">
                <div class="card book-search-card" style="cursor: pointer;">
                    <div class="card-body">
                        <div class="row">
                            <div class="col-auto">
                                ${book.image ? 
                                    `<img src="${book.image}" alt="${title}" style="width: 80px; height: 110px; object-fit: cover;">` :
                                    '<div style="width: 80px; height: 110px; background: #f0f0f0; display: flex; align-items: center; justify-content: center;"><i class="bi bi-book text-muted" style="font-size: 2rem;"></i></div>'
                                }
                            </div>
                            <div class="col">
                                <h6 class="card-title mb-1">${title}</h6>
                                <p class="text-muted small mb-1">
                                    ${author} | ${publisher}
                                    ${book.pubdate ? ` | ${book.pubdate.substring(0, 4)}년` : ''}
                                </p>
                                ${book.isbn ? `<p class="text-muted small mb-1">ISBN: ${book.isbn}</p>` : ''}
                                ${book.discount ? `<p class="mb-1"><strong>${parseInt(book.discount).toLocaleString()}원</strong></p>` : ''}
                                ${description ? `<p class="text-muted small mb-0" style="overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;">${description}</p>` : ''}
                            </div>
                            <div class="col-auto align-self-center">
                                <button class="btn btn-sm btn-primary select-book-btn">선택</button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `);
        
        // 책 데이터 저장
        card.find('.book-search-card').data('book', {
            title: title,
            author: author,
            publisher: publisher,
            isbn: book.isbn,
            image: book.image,
            price: book.discount ? parseInt(book.discount) : null,
            pubdate: book.pubdate,
            publicationYear: book.pubdate ? parseInt(book.pubdate.substring(0, 4)) : null
        });
        
        return card;
    }
    
    // 책 선택 이벤트
    $(document).on('click', '.select-book-btn', function(e) {
        e.stopPropagation();
        const bookData = $(this).closest('.book-search-card').data('book');
        selectBook(bookData);
    });
    
    // 카드 전체 클릭도 선택으로 처리
    $(document).on('click', '.book-search-card', function() {
        const bookData = $(this).data('book');
        selectBook(bookData);
    });
    
    // 책 선택 처리
    function selectBook(bookData) {
        // 로딩 표시
        const $modal = $('#bookSearchModal');
        const $modalContent = $modal.find('.modal-content');
        const originalContent = $modalContent.html();
        
        $modalContent.html(`
            <div class="modal-body text-center py-5">
                <div class="spinner-border text-primary mb-3" role="status">
                    <span class="visually-hidden">처리 중...</span>
                </div>
                <p>책 정보를 저장하고 있습니다...</p>
            </div>
        `);
        
        // 서버에 책 정보 전송
        $.ajax({
            url: '/api/books/select',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(bookData),
            success: function(response) {
                if (response.bookId) {
                    selectedBook = bookData;
                    
                    // 선택된 책 정보 표시
                    $('#selectedBookTitle').text(bookData.title);
                    $('#selectedBookAuthor').text(bookData.author);
                    $('#selectedBookPublisher').text(bookData.publisher);
                    
                    // 책 표지 이미지 표시
                    if (bookData.image && bookData.image.trim() !== '') {
                        $('#selectedBookImage')
                            .attr('src', bookData.image)
                            .css({'display': 'block', 'z-index': '2'});
                        $('#selectedBookPlaceholder').css({'display': 'none', 'z-index': '1'});
                    } else {
                        $('#selectedBookImage').css({'display': 'none', 'z-index': '1'});
                        $('#selectedBookPlaceholder').css({'display': 'flex', 'z-index': '2'});
                    }
                    
                    // bookId 설정
                    $('#bookId').val(response.bookId);
                    $('#removeBook').val('false'); // 책 선택 시 removeBook을 false로
                    
                    // UI 업데이트
                    $('#selectedBookInfo').show();
                    $('#bookSearchBtnDiv').hide();
                    
                    // 모달 닫기
                    $modal.modal('hide');
                } else {
                    alert('책 정보 저장에 실패했습니다.');
                    $modalContent.html(originalContent);
                }
            },
            error: function(xhr, status, error) {
                alert('책 정보 저장 중 오류가 발생했습니다.');
                $modalContent.html(originalContent);
                console.error('책 선택 오류:', error);
            }
        });
    }
    
    // 페이지네이션 업데이트
    function updatePagination(total, currentPage) {
        const pagination = $('#bookPagination');
        pagination.empty();
        
        const totalPages = Math.ceil(total / 10);
        const maxPages = Math.min(totalPages, 10); // 최대 10페이지만 표시
        
        // 이전 버튼
        if (currentPage > 1) {
            pagination.append(`
                <li class="page-item">
                    <a class="page-link" href="#" data-page="${currentPage - 1}" aria-label="Previous">
                        <span aria-hidden="true">&laquo;</span>
                    </a>
                </li>
            `);
        }
        
        // 페이지 번호
        for (let i = 1; i <= maxPages; i++) {
            const active = i === currentPage ? 'active' : '';
            pagination.append(`
                <li class="page-item ${active}">
                    <a class="page-link" href="#" data-page="${i}">${i}</a>
                </li>
            `);
        }
        
        // 다음 버튼
        if (currentPage < totalPages && currentPage < 10) {
            pagination.append(`
                <li class="page-item">
                    <a class="page-link" href="#" data-page="${currentPage + 1}" aria-label="Next">
                        <span aria-hidden="true">&raquo;</span>
                    </a>
                </li>
            `);
        }
    }
    
    // 페이지네이션 클릭 이벤트
    $(document).on('click', '#bookPagination .page-link', function(e) {
        e.preventDefault();
        const page = $(this).data('page');
        searchBooks(page);
    });
    
    // 모달 초기화
    $('#bookSearchModal').on('hidden.bs.modal', function() {
        $('#bookSearchInput').val('');
        $('#bookResultsContainer').empty();
        $('#bookSearchResults').hide();
        $('#noBookResults').hide();
        $('#bookSearchLoading').hide();
        currentPage = 1;
        currentQuery = '';
    });
});