package com.hot6.backend.chat;


import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.hot6.backend.chat.model.ChatDto;
import com.hot6.backend.chat.model.ChatMessageType;
import com.hot6.backend.chat.service.ChatRoomParticipantService;
import com.hot6.backend.chat.service.ChatRoomService;
import com.hot6.backend.config.SecurityConfig;
import com.hot6.backend.pet.PetController;
import com.hot6.backend.user.model.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(controllers = {ChatController.class, PetController.class})
//@Import(ChatController.class)
class ChatControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    ChatRoomService chatRoomService;

    @MockitoBean
    SecurityConfig securityConfig;

    @MockitoBean
    ChatRoomParticipantService chatRoomParticipantService;

    @Autowired
    ApplicationContext applicationContext;


    @Test
    void checkChatController() {
        System.out.println(applicationContext.getBeansOfType(ChatController.class));
        System.out.println("chatController bean exists = " + applicationContext.containsBean("chatController"));
    }

    @Test
    void printControllerBean() {
        Object bean = applicationContext.getBean("createBean");

        System.out.println(bean.getClass());
    }


    @Test
    void printAllBeans() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        Arrays.sort(beanNames);

        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            System.out.println(beanName + " -> " + bean.getClass().getName());
        }
    }

    @Test
    void given_참여자_인증유저_when_lastMessageId로_이전_메시지_조회_then_200과_SUCCESS와_메시지Slice_반환() throws Exception {

        // given
        Long chatRoomIdx = 10L;
        Long userIdx = 2L;
        Long lastMessageId = 123L;
        int size = 20;

        User user = new User();          // ✅ 실제 User 엔티티/도메인에 맞게 세팅
        user.setIdx(userIdx);

        ChatDto.ChatElement e1 = ChatDto.ChatElement.builder()
                .idx(124L)
                .type(ChatMessageType.TEXT)
                .content("test")
                .build();

        ChatDto.ChatElement e2 = ChatDto.ChatElement.builder()
                .idx(125L)
                .type(ChatMessageType.TEXT)
                .content("test fed se")
                .build();

        Slice<ChatDto.ChatElement> slice =
                new SliceImpl<>(List.of(e1, e2), PageRequest.of(0, size), true);

        given(chatRoomService.getChatMessages(chatRoomIdx, userIdx, lastMessageId, size))
                .willReturn(slice);

        RequestPostProcessor auth = authenticationTokenWithPrincipal(user);


        // when & then
        mockMvc.perform(
                        get("/chat/chatroom/{chatRoomIdx}/chat", chatRoomIdx)
                                .param("lastMessageId", String.valueOf(lastMessageId))
                                .param("size", String.valueOf(size))
                                .with(auth)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.isSuccess").value("true"))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
                .andExpect(jsonPath("$.result.content").isArray())
                .andExpect(jsonPath("$.result.content.length()").value(2))
                .andExpect(jsonPath("$.result.content[0].messageId").value(124L))
                .andExpect(jsonPath("$.result.hasNext").value(true));
    }


    private static RequestPostProcessor authenticationTokenWithPrincipal(User userPrincipal) {
        Authentication token = new UsernamePasswordAuthenticationToken(
                userPrincipal,  // principal = @AuthenticationPrincipal로 들어갈 값
                null,
                List.of()       // 권한 필요하면 여기에 GrantedAuthority 넣기
        );


        return authentication(token);
    }



    @Configuration
    public static class ChatControllerTestConfig {


        @Bean
        public Integer createBean() {
            System.out.println("test");
            Integer test = Integer.valueOf(12);
            return test;
        }
    }


}