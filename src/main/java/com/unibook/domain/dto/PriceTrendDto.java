package com.unibook.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class PriceTrendDto {
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartData {
        private List<DataPoint> availableAndReserved; // 판매중+예약중
        private List<DataPoint> completed;           // 거래완료
        private BookInfo bookInfo;                   // 책 정보
        private boolean hasData;                     // 데이터 존재 여부
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPoint {
        private String date;        // ISO 형태의 날짜 (Chart.js용)
        private Integer price;      // 가격
        private String status;      // 상태 (Chart.js tooltip용)
        private Long postId;        // 게시글 ID (링크용)
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookInfo {
        private Long bookId;
        private String title;
        private String author;
        private String isbn;
    }
}