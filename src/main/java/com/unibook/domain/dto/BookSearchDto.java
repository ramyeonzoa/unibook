package com.unibook.domain.dto;

import lombok.AllArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
public class BookSearchDto {
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        private String query;
        
        @Builder.Default
        private int display = 10;
        
        @Builder.Default
        private int start = 1;
        
        @Builder.Default
        private String sort = "sim"; // sim(정확도), date(출간일)
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private String lastBuildDate;
        private int total;
        private int start;
        private int display;
        private List<Item> items;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        private String title;
        private String link;
        private String image;
        private String author;
        private String discount;
        private String publisher;
        private String isbn;
        private String description;
        private String pubdate;
        
        // 클린한 데이터 반환을 위한 메서드
        public String getCleanTitle() {
            return title != null ? title.replaceAll("<[^>]*>", "") : "";
        }
        
        public String getCleanAuthor() {
            return author != null ? author.replaceAll("<[^>]*>", "") : "";
        }
        
        public String getCleanPublisher() {
            return publisher != null ? publisher.replaceAll("<[^>]*>", "") : "";
        }
        
        public Integer getPrice() {
            try {
                return discount != null ? Integer.parseInt(discount) : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        public Integer getPublicationYear() {
            if (pubdate != null && pubdate.length() >= 4) {
                try {
                    return Integer.parseInt(pubdate.substring(0, 4));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }
    }
}