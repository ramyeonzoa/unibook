package com.unibook.controller;

import com.unibook.service.BookService;
import com.unibook.service.PostService;
import com.unibook.service.SchoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {
    
    private final SchoolService schoolService;
    private final BookService bookService;
    private final PostService postService;
    
    @Value("${app.home.popular-books-limit}")
    private int popularBooksLimit;
    
    @Value("${app.home.recent-posts-limit}")
    private int recentPostsLimit;
    
    @GetMapping("/")
    public String home(Model model) {
        // DTO를 사용하여 데이터 전달
        model.addAttribute("schools", schoolService.getAllSchoolDtos());
        model.addAttribute("popularBooks", bookService.getPopularBookDtos(popularBooksLimit));
        model.addAttribute("recentPosts", postService.getRecentPostDtos(recentPostsLimit));
        
        return "index";
    }
}