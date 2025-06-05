package com.unibook.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unibook.common.AppConstants;
import com.unibook.domain.entity.Book;
import com.unibook.domain.entity.Post;
import com.unibook.domain.entity.Subject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.HashMap;
import java.util.Map;

/**
 * Post 폼 관련 데이터 빌더 유틸리티
 * PostController의 중복된 Model 설정 및 JSON 변환 로직을 통합
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostFormDataBuilder {
    
    private final ObjectMapper objectMapper;
    
    /**
     * 게시글 폼에 필요한 공통 Model 속성들을 설정
     * 
     * @param model Model 객체
     * @param isEdit 수정 모드인지 여부
     */
    public void addFormAttributes(Model model, boolean isEdit) {
        model.addAttribute("productTypes", Post.ProductType.values());
        model.addAttribute("transactionMethods", Post.TransactionMethod.values());
        model.addAttribute("statuses", Post.PostStatus.values());
        model.addAttribute("maxImages", AppConstants.MAX_IMAGES_PER_POST);
        model.addAttribute("isEdit", isEdit);
    }
    
    /**
     * Book 객체를 JSON 문자열로 변환
     * 
     * @param book Book 객체 (null 가능)
     * @return JSON 문자열 또는 "null"
     */
    public String buildBookJson(Book book) {
        if (book == null) {
            return "null";
        }
        
        try {
            Map<String, Object> bookData = new HashMap<>();
            bookData.put("bookId", book.getBookId());
            bookData.put("title", book.getTitle());
            bookData.put("author", book.getAuthor());
            bookData.put("publisher", book.getPublisher());
            bookData.put("isbn", book.getIsbn());
            bookData.put("imageUrl", book.getImageUrl());
            
            return objectMapper.writeValueAsString(bookData);
        } catch (Exception e) {
            log.error("책 정보 JSON 변환 실패: bookId={}", book.getBookId(), e);
            return "null";
        }
    }
    
    /**
     * Subject 객체를 JSON 문자열로 변환
     * 
     * @param subject Subject 객체 (null 가능)
     * @param takenYear 수강년도 (null 가능)
     * @param takenSemester 수강학기 (null 가능)
     * @return JSON 문자열 또는 "null"
     */
    public String buildSubjectJson(Subject subject, Integer takenYear, Subject.Semester takenSemester) {
        if (subject == null) {
            return "null";
        }
        
        try {
            Map<String, Object> subjectData = new HashMap<>();
            subjectData.put("subjectId", subject.getSubjectId());
            subjectData.put("subjectName", subject.getSubjectName());
            
            // Professor 정보 안전하게 추가
            if (subject.getProfessor() != null) {
                subjectData.put("professorName", subject.getProfessor().getProfessorName());
                
                // Department 정보 안전하게 추가
                if (subject.getProfessor().getDepartment() != null) {
                    subjectData.put("departmentName", subject.getProfessor().getDepartment().getDepartmentName());
                }
            }
            
            subjectData.put("year", takenYear);
            subjectData.put("semester", takenSemester != null ? takenSemester.name() : null);
            subjectData.put("type", subject.getType().name());
            
            return objectMapper.writeValueAsString(subjectData);
        } catch (Exception e) {
            log.error("과목 정보 JSON 변환 실패: subjectId={}", subject.getSubjectId(), e);
            return "null";
        }
    }
    
    /**
     * 게시글 폼 에러 처리용 Model 설정
     * 에러 발생 시 폼을 다시 보여줄 때 필요한 모든 데이터를 설정
     * 
     * @param model Model 객체
     * @param post 기존 Post 객체
     * @param isEdit 수정 모드인지 여부
     */
    public void addFormAttributesForError(Model model, Post post, boolean isEdit) {
        // 기본 폼 속성 설정
        addFormAttributes(model, isEdit);
        
        // Post 객체 추가
        model.addAttribute("post", post);
        
        // JSON 데이터 설정
        model.addAttribute("selectedBookJson", buildBookJson(post.getBook()));
        model.addAttribute("selectedSubjectJson", buildSubjectJson(
                post.getSubject(), post.getTakenYear(), post.getTakenSemester()));
    }
}