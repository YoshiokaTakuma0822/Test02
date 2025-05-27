package com.example.demo.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.dto.UserDto;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ActiveUsersService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ActiveUsersService activeUsersService;

    @Test
    @DisplayName("GET /api/users should return all users")
    void testGetAllUsers() throws Exception {
        // Create test users via API
        User user1 = createUserViaApi("John Doe", "john@example.com", 1L);
        User user2 = createUserViaApi("Jane Smith", "jane@example.com", 2L);
        List<User> users = Arrays.asList(user1, user2);

        when(userService.getAllUsers()).thenReturn(users);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("John Doe")))
                .andExpect(jsonPath("$[0].email", is("john@example.com")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].name", is("Jane Smith")))
                .andExpect(jsonPath("$[1].email", is("jane@example.com")));
    }

    @Test
    @DisplayName("GET /api/users should return empty list when no users exist")
    void testGetAllUsers_Empty() throws Exception {
        when(userService.getAllUsers()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/users/{id} should return a user when it exists")
    void testGetUserById_UserExists() throws Exception {
        User user = createUserViaApi("John Doe", "john@example.com", 1L);
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("John Doe")))
                .andExpect(jsonPath("$.email", is("john@example.com")));
    }

    @Test
    @DisplayName("GET /api/users/{id} should return 404 when user doesn't exist")
    void testGetUserById_UserDoesNotExist() throws Exception {
        when(userService.getUserById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/users should create a new user")
    void testCreateUser() throws Exception {
        // When
        when(userService.saveUser(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            try {
                var idField = User.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(savedUser, 3L);
                idField.setAccessible(false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return savedUser;
        });

        // Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New User\",\"email\":\"new@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.name", is("New User")))
                .andExpect(jsonPath("$.email", is("new@example.com")));

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New User\",\"email\":\"new@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(3)))
                .andExpect(jsonPath("$.name", is("New User")))
                .andExpect(jsonPath("$.email", is("new@example.com")));
    }

    @Test
    @DisplayName("POST /api/users with invalid JSON should return 400 Bad Request")
    void testCreateUser_InvalidJson() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json content}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/users with empty content should return 400 Bad Request")
    void testCreateUser_EmptyContent() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/users/add-default should add a default user")
    void testAddDefaultUser() throws Exception {
        // Create a user that will be returned as default
        User defaultUser = createUserViaApi("Default User", "default@example.com", 4L);
        when(userService.createDefaultUser()).thenReturn(defaultUser);

        mockMvc.perform(get("/api/users/add-default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(4)))
                .andExpect(jsonPath("$.name", is("Default User")))
                .andExpect(jsonPath("$.email", is("default@example.com")));
    }

    @Test
    @DisplayName("GET /api/users/ensure-default-users should ensure default users exist")
    void testEnsureDefaultUsers() throws Exception {
        // Create users that will be returned as default users
        User user1 = createUserViaApi("John Doe", "john@example.com", 1L);
        User user2 = createUserViaApi("Jane Smith", "jane@example.com", 2L);
        List<User> defaultUsers = Arrays.asList(user1, user2);
        when(userService.ensureDefaultUsers()).thenReturn(defaultUsers);

        mockMvc.perform(get("/api/users/ensure-default-users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("John Doe")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].name", is("Jane Smith")));

        verify(userService, times(1)).ensureDefaultUsers();
    }

    @Nested
    @DisplayName("GET /api/users/active のテスト")
    class GetActiveUsersTests {

        @Test
        @DisplayName("アクティブなユーザーが存在する場合、すべてのアクティブユーザーを返すこと")
        void shouldReturnAllActiveUsers() throws Exception {
            List<User> activeUsers = Arrays.asList(
                    createUserViaApi("John Doe", "john@example.com", 1L),
                    createUserViaApi("Jane Smith", "jane@example.com", 2L));
            List<UserDto> activeDtos = activeUsers.stream()
                    .map(UserDto::fromEntity)
                    .collect(Collectors.toList());

            when(activeUsersService.getActiveUsers()).thenReturn(activeDtos);

            mockMvc.perform(get("/api/users/active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].name", is("John Doe")))
                    .andExpect(jsonPath("$[0].email", is("john@example.com")))
                    .andExpect(jsonPath("$[1].id", is(2)))
                    .andExpect(jsonPath("$[1].name", is("Jane Smith")))
                    .andExpect(jsonPath("$[1].email", is("jane@example.com")));

            verify(activeUsersService, times(1)).getActiveUsers();
        }

        @Test
        @DisplayName("アクティブなユーザーが存在しない場合、空のリストを返すこと")
        void shouldReturnEmptyListWhenNoActiveUsers() throws Exception {
            when(activeUsersService.getActiveUsers()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/users/active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(activeUsersService, times(1)).getActiveUsers();
        }

        @Test
        @DisplayName("サービスがエラーを投げた場合、500エラーを返すこと")
        void shouldReturn500WhenServiceThrowsException() throws Exception {
            when(activeUsersService.getActiveUsers()).thenThrow(new RuntimeException("Service error"));

            mockMvc.perform(get("/api/users/active"))
                    .andExpect(status().isInternalServerError());

            verify(activeUsersService, times(1)).getActiveUsers();
        }
    }

    private User createUserViaApi(String name, String email, Long expectedId) throws Exception {
        // When
        when(userService.saveUser(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            try {
                var idField = User.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(savedUser, expectedId);
                idField.setAccessible(false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return savedUser;
        });

        // Perform the request and get response
        String response = mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"name\":\"%s\",\"email\":\"%s\"}", name, email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(expectedId.intValue())))
                .andExpect(jsonPath("$.name", is(name)))
                .andExpect(jsonPath("$.email", is(email)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Reset the mock to avoid interference with other tests
        reset(userService);

        // Parse response and return user object
        ObjectMapper mapper = new ObjectMapper();
        // readValue cannot return null for a concrete class like User
        return mapper.readValue(response, User.class);
    }
}
