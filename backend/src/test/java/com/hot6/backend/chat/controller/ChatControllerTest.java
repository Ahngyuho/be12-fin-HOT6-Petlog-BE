package com.hot6.backend.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hot6.backend.chat.ChatController;
import com.hot6.backend.chat.model.ChatDto;
import com.hot6.backend.chat.service.ChatRoomParticipantService;
import com.hot6.backend.chat.service.ChatRoomService;
import com.hot6.backend.common.exception.GlobalExceptionHandler;
import com.hot6.backend.user.model.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@Import({GlobalExceptionHandler.class, ChatControllerTest.TestSecurityConfig.class}) // 위의 예외 핸들러를 함께 로드
public class ChatControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;


    //권장 방식?
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    ChatRoomService chatRoomService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    ChatRoomParticipantService chatRoomParticipant;

    // --- 헬퍼: 내 도메인 User를 principal로 주입
    private Authentication authWithUser(long userId) {
        User principal = new User();
        principal.setIdx(userId);
        // 권한은 필요 없으면 빈 리스트
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    @Test
    void createGroupChat_success_withStartDateTime() throws Exception {
        // given
        ChatDto.CreateChatRoomRequest req = ChatDto.CreateChatRoomRequest.builder()
                .title("테스트방")
                .hashtags(List.of("#tag1", "#tag2"))
                .startDateTime("2025-09-01T09:00:00")
                .maxParticipants(10)
                .build();

        String json = objectMapper.writeValueAsString(req);

        // when & then
        var captorReq = ArgumentCaptor.forClass(ChatDto.CreateChatRoomRequest.class);
        var captorUserId = ArgumentCaptor.forClass(Long.class);
        var captorLdt = ArgumentCaptor.forClass(LocalDateTime.class);

        mockMvc.perform(post("/chat")
                        .with(csrf())
                        .with(authentication(authWithUser(42L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string("채팅방 생성 완료"));

        verify(chatRoomService, times(1))
                .createChatRoom(captorReq.capture(), captorUserId.capture(), captorLdt.capture());

        assertThat(captorUserId.getValue()).isEqualTo(42L);
        assertThat(captorReq.getValue().getTitle()).isEqualTo("테스트방");
        assertThat(captorReq.getValue().getHashtags()).containsExactly("#tag1", "#tag2");
        assertThat(captorLdt.getValue()).isEqualTo(LocalDateTime.of(2025, 9, 1, 9, 0, 0));
    }



    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            // 테스트에선 간단히 모두 허용 + CSRF 필요시 테스트에서 .with(csrf())
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }
}
