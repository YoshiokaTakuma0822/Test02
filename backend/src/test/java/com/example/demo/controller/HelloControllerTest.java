package com.example.demo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import com.example.demo.repository.UserRepository;

@WebMvcTest(HelloController.class)
public class HelloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("GET /hello should return 'Hello, World!' string")
    void testHelloEndpoint() throws Exception {
        mockMvc.perform(get("/hello"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string("Hello, World!"));
    }

    @Test
    @DisplayName("GET /hello with accept header should return correct content type")
    void testHelloEndpointWithAcceptHeader() throws Exception {
        mockMvc.perform(get("/hello")
                .header("Accept", "text/plain"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andExpect(content().string("Hello, World!"));
    }

    @Test
    @DisplayName("GET /invalid-path should return 404")
    void testInvalidPath() throws Exception {
        mockMvc.perform(get("/invalid-path"))
                .andExpect(status().isNotFound());
    }
}
