package com.unibook.service;

import com.unibook.domain.entity.Post;
import com.unibook.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    
    private final PostRepository postRepository;
    
    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }
    
    public Optional<Post> getPostById(Long id) {
        return postRepository.findById(id);
    }
    
    public List<Post> getRecentPosts(int limit) {
        return postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).getContent();
    }
    
    public List<Post> getPostsBySchool(Long schoolId) {
        return postRepository.findByUser_School_SchoolId(schoolId);
    }
    
    public List<Post> getPostsByStatus(Post.PostStatus status) {
        return postRepository.findByStatus(status);
    }
    
    @Transactional
    public Post savePost(Post post) {
        return postRepository.save(post);
    }
    
    @Transactional
    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }
}