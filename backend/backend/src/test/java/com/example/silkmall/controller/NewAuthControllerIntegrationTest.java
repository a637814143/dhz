package com.example.silkmall.controller;

import com.example.silkmall.dto.LoginDTO;
import com.example.silkmall.dto.RegisterDTO;
import com.example.silkmall.entity.Consumer;
import com.example.silkmall.repository.NewConsumerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:authdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.main.allow-circular-references=true"
})
class NewAuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NewConsumerRepository consumerRepository;

    @Test
    void registerAndLoginConsumerAccount() throws Exception {
        RegisterDTO register = new RegisterDTO();
        register.setUsername("demoUser");
        register.setPassword("secret123");
        register.setConfirmPassword("secret123");
        register.setEmail("demo@example.com");
        register.setPhone("18800001111");
        register.setUserType("consumer");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        Consumer saved = consumerRepository.findByUsername("demoUser").orElseThrow();
        assertThat(saved.getPassword()).startsWith("$2");
        assertThat(saved.isEnabled()).isTrue();

        MvcResult captchaResult = mockMvc.perform(get("/api/auth/captcha"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> captchaBody = objectMapper.readValue(
                captchaResult.getResponse().getContentAsByteArray(),
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> captchaData = (Map<String, Object>) captchaBody.get("data");
        String challengeId = (String) captchaData.get("challengeId");
        String question = (String) captchaData.get("question");
        int solution = evaluate(question);

        LoginDTO login = new LoginDTO();
        login.setUsername("demoUser");
        login.setPassword("secret123");
        login.setChallengeId(challengeId);
        login.setVerificationCode(Integer.toString(solution));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk());
    }

    private int evaluate(String question) {
        String sanitized = question.replace("= ?", "").replace("=", "").replace("?", "").trim();
        String[] parts;
        if (sanitized.contains("×")) {
            parts = sanitized.split("×");
            return Integer.parseInt(parts[0].trim()) * Integer.parseInt(parts[1].trim());
        }
        parts = sanitized.split("\\+");
        return Integer.parseInt(parts[0].trim()) + Integer.parseInt(parts[1].trim());
    }
}
