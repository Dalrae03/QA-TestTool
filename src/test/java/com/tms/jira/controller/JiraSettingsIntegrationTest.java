package com.tms.jira.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.jira.dto.JiraSettingsRequest;
import com.tms.jira.repository.JiraSettingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class JiraSettingsIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JiraSettingRepository jiraSettingRepository;

    @AfterEach
    void tearDown() {
        jiraSettingRepository.deleteAll();
    }

    @Test
    void 설정을_저장하면_토큰은_마스킹되고_값은_보존된다() throws Exception {
        JiraSettingsRequest request = new JiraSettingsRequest(
                "https://acme.atlassian.net", "qa@acme.com", "secret-token", "TMS", null, true);

        mockMvc.perform(put("/api/jira/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseUrl").value("https://acme.atlassian.net"))
                .andExpect(jsonPath("$.email").value("qa@acme.com"))
                .andExpect(jsonPath("$.projectKey").value("TMS"))
                .andExpect(jsonPath("$.hasToken").value(true))
                .andExpect(jsonPath("$.configured").value(true))
                // 토큰 값 자체는 응답에 절대 실리지 않는다.
                .andExpect(jsonPath("$.apiToken").doesNotExist());

        mockMvc.perform(get("/api/jira/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasToken").value(true))
                .andExpect(jsonPath("$.email").value("qa@acme.com"));
    }

    @Test
    void 토큰을_비워서_다시_저장하면_기존_토큰을_유지한다() throws Exception {
        mockMvc.perform(put("/api/jira/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JiraSettingsRequest(
                                "https://acme.atlassian.net", "qa@acme.com", "secret-token", "TMS", null, true))))
                .andExpect(status().isOk());

        // 토큰 없이 이메일만 변경 — 토큰은 유지되어야 configured=true.
        mockMvc.perform(put("/api/jira/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JiraSettingsRequest(
                                "https://acme.atlassian.net", "lead@acme.com", null, "TMS", null, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("lead@acme.com"))
                .andExpect(jsonPath("$.hasToken").value(true))
                .andExpect(jsonPath("$.configured").value(true));
    }

    @Test
    void 미설정_상태에서_연결테스트하면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/jira/settings/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
