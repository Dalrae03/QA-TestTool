package com.tms.user.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.user.dto.UserRequest;
import com.tms.user.entity.UserRole;
import com.tms.user.repository.TmsUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired TmsUserRepository userRepository;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void shouldManageUsersAndRoles() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRequest(
                                "김QA", "QA@example.com", UserRole.QA_LEAD, true
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("김QA"))
                .andExpect(jsonPath("$.email").value("qa@example.com"))
                .andExpect(jsonPath("$.role").value("QA_LEAD"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();

        long userId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRequest(
                                "김담당", "owner@example.com", UserRole.DEVELOPER, false
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("김담당"))
                .andExpect(jsonPath("$.role").value("DEVELOPER"))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value("owner@example.com"));
    }

    @Test
    void shouldRejectDuplicateEmailAndDeactivateUser() throws Exception {
        UserRequest request = new UserRequest("담당자", "owner@example.com", UserRole.QA, true);
        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        long userId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRequest(
                                "중복", "OWNER@example.com", UserRole.QA, true
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 등록된 이메일입니다: owner@example.com"));

        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void shouldValidateUserRequest() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserRequest(
                                "", "not-email", null, true
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.role").exists());
    }
}
