package com.unibook.service;

import com.unibook.common.AppConstants;
import com.unibook.domain.dto.BookSearchDto;
import com.unibook.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookSearchService {
    
    private final RestTemplate restTemplate;
    
    @Value("${naver.api.client-id}")
    private String clientId;
    
    @Value("${naver.api.client-secret}")
    private String clientSecret;
    
    @Value("${naver.api.book-search-url}")
    private String bookSearchUrl;
    
    @Cacheable(value = "bookSearch", key = "#query + '_' + #page + '_' + #size")
    @Retryable(
            value = {ResourceAccessException.class, HttpServerErrorException.class},
            maxAttempts = AppConstants.NAVER_API_MAX_RETRY_ATTEMPTS,
            backoff = @Backoff(
                    delay = AppConstants.NAVER_API_RETRY_DELAY,
                    multiplier = AppConstants.NAVER_API_RETRY_MULTIPLIER
            )
    )
    public BookSearchDto.Response searchBooks(String query, int page, int size) {
        if (query == null || query.trim().isEmpty()) {
            throw new ValidationException("검색어를 입력해주세요.");
        }
        
        // 페이지와 크기 검증
        page = Math.max(1, page);
        size = size <= 0 ? AppConstants.NAVER_BOOK_SEARCH_DEFAULT_DISPLAY : 
               Math.min(size, AppConstants.NAVER_BOOK_SEARCH_MAX_DISPLAY);
        
        try {
            // URL 구성
            URI uri = UriComponentsBuilder.fromHttpUrl(bookSearchUrl)
                    .queryParam("query", query.trim())
                    .queryParam("display", size)
                    .queryParam("start", (page - 1) * size + 1)  // 네이버 API는 1부터 시작
                    .queryParam("sort", "sim")  // 정확도순
                    .encode(StandardCharsets.UTF_8)
                    .build()
                    .toUri();
            
            // 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Naver-Client-Id", clientId);
            headers.set("X-Naver-Client-Secret", clientSecret);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            
            // 요청 생성
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            // API 호출
            ResponseEntity<BookSearchDto.Response> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<BookSearchDto.Response>() {}
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.debug("책 검색 성공: 검색어={}, 결과수={}", query, 
                        response.getBody().getItems() != null ? response.getBody().getItems().size() : 0);
                return response.getBody();
            } else {
                log.error("네이버 책 검색 API 호출 실패: {}", response.getStatusCode());
                throw new ValidationException("책 검색에 실패했습니다.");
            }
            
        } catch (HttpClientErrorException e) {
            // 4xx 에러는 재시도하지 않음
            log.error("책 검색 중 클라이언트 오류 발생: query={}, status={}", query, e.getStatusCode());
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new ValidationException("잘못된 검색 요청입니다.");
            } else if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new ValidationException("API 인증에 실패했습니다.");
            } else {
                throw new ValidationException("책 검색에 실패했습니다.");
            }
        } catch (ResourceAccessException | HttpServerErrorException e) {
            // 네트워크 오류 및 5xx 에러는 재시도
            log.error("책 검색 중 서버 오류 발생: query={}", query, e);
            throw e;
        } catch (ValidationException e) {
            throw e; // 비즈니스 예외는 그대로 전달
        } catch (Exception e) {
            log.error("책 검색 중 예상치 못한 오류 발생: query={}", query, e);
            throw new ValidationException("책 검색에 실패했습니다.");
        }
    }
}