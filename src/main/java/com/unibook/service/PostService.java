package com.unibook.service;

import com.unibook.domain.dto.PostResponseDto;
import com.unibook.domain.entity.Post;
import com.unibook.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    
    private final PostRepository postRepository;
    
    public List<Post> getAllPosts() {
        // TODO: 페이징 필요 - 전체 조회는 위험
        return postRepository.findAllWithDetails();
    }
    
    public Optional<Post> getPostById(Long id) {
        // Fetch Join을 사용하여 연관 데이터를 한 번에 조회
        return postRepository.findByIdWithDetails(id);
    }
    
    public List<Post> getRecentPosts(int limit) {
        // Fetch Join을 사용하여 N+1 문제 해결
        return postRepository.findRecentPostsWithDetails(PageRequest.of(0, limit));
    }
    
    public List<Post> findPostsBySchoolId(Long schoolId) {
        // Fetch Join을 사용하여 N+1 문제 해결
        return postRepository.findBySchoolIdWithDetails(schoolId);
    }
    
    public List<Post> findPostsByStatus(Post.PostStatus status) {
        return postRepository.findByStatusWithDetails(status);
    }
    
    @Transactional
    public Post savePost(Post post) {
        return postRepository.save(post);
    }
    
    @Transactional
    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }
    
    // DTO 반환 메서드
    public List<PostResponseDto> getRecentPostDtos(int limit) {
        return getRecentPosts(limit).stream()
                .map(PostResponseDto::listFrom)  // 목록용 간단한 DTO 사용
                .collect(Collectors.toList());
    }
}