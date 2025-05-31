package com.unibook.controller;

import com.unibook.domain.dto.ChatDto;
import com.unibook.security.UserPrincipal;
import com.unibook.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {
    
    private final ChatService chatService;
    
    /**
     * 채팅방 목록 페이지
     */
    @GetMapping
    public String chatRoomList(@AuthenticationPrincipal UserPrincipal userPrincipal, Model model) {
        List<ChatDto.ChatRoomListResponse> chatRooms = chatService.getChatRoomsByUserId(
            userPrincipal.getUserId());
        
        model.addAttribute("chatRooms", chatRooms);
        model.addAttribute("totalUnreadCount", chatService.getTotalUnreadCount(userPrincipal.getUserId()));
        
        return "chat/list";
    }
    
    /**
     * 1:1 채팅 페이지
     */
    @GetMapping("/rooms/{chatRoomId}")
    public String chatRoom(@PathVariable Long chatRoomId,
                          @AuthenticationPrincipal UserPrincipal userPrincipal,
                          Model model) {
        
        ChatDto.ChatRoomDetailResponse chatRoom = chatService.getChatRoomDetail(
            chatRoomId, userPrincipal.getUserId());
        
        model.addAttribute("chatRoom", chatRoom);
        model.addAttribute("currentUserId", userPrincipal.getUserId());
        model.addAttribute("currentUserName", userPrincipal.getName());
        
        return "chat/room";
    }
}