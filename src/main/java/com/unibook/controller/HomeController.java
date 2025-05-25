package com.unibook.controller;

import com.unibook.service.BookService;
import com.unibook.service.PostService;
import com.unibook.service.SchoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {
    
    private final SchoolService schoolService;
    private final BookService bookService;
    private final PostService postService;
    
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("schools", schoolService.getAllSchools());
        model.addAttribute("popularBooks", bookService.getPopularBooks(8));
        model.addAttribute("recentPosts", postService.getRecentPosts(5));
        
        return "index";
    }
}