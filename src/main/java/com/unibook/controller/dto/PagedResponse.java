package com.unibook.controller.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이징된 응답을 위한 표준 DTO
 * @param <T> 응답 아이템 타입
 */
public class PagedResponse<T> {
    private List<T> items;
    private long total;
    private int page;
    private int size;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
    
    public PagedResponse() {}
    
    public PagedResponse(Page<T> page, int requestedPage) {
        this.items = page.getContent();
        this.total = page.getTotalElements();
        this.page = requestedPage; // 1-based 페이지 번호
        this.size = page.getSize();
        this.totalPages = page.getTotalPages();
        this.hasNext = page.hasNext();
        this.hasPrevious = page.hasPrevious();
    }
    
    public static <T> PagedResponse<T> of(Page<T> page, int requestedPage) {
        return new PagedResponse<>(page, requestedPage);
    }
    
    // Getters and Setters
    public List<T> getItems() { return items; }
    public void setItems(List<T> items) { this.items = items; }
    
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    
    public boolean isHasNext() { return hasNext; }
    public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }
    
    public boolean isHasPrevious() { return hasPrevious; }
    public void setHasPrevious(boolean hasPrevious) { this.hasPrevious = hasPrevious; }
}