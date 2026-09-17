package com.volako.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginAccessProtectedRouteThenRefresh() throws Exception {
        String phoneNumber = "0340" + (System.nanoTime() % 1_000_000);
        String email = "nija+" + System.nanoTime() + "@volako.mg";
        String registerPayload = """
                {"firstName": "Nija", "lastName": "Rakoto", "phoneNumber": "%s", "email": "%s", "password": "SuperSecret123"}
                """.formatted(phoneNumber, email);

        String registerBody = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.phoneNumber").value(phoneNumber))
                .andReturn().getResponse().getContentAsString();

        JsonNode registerJson = objectMapper.readTree(registerBody);
        String accessToken = registerJson.get("accessToken").asText();
        String refreshToken = registerJson.get("refreshToken").asText();

        // Protected route without token is rejected.
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        // Protected route with the access token succeeds.
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value(phoneNumber));

        // Login with the same credentials works.
        String loginPayload = """
                {"phoneNumber": "%s", "password": "SuperSecret123"}
                """.formatted(phoneNumber);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());

        // Refreshing rotates the token: the old refresh token can only be used once.
        String refreshPayload = """
                {"refreshToken": "%s"}
                """.formatted(refreshToken);
        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());

        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshPayload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registeringTwiceWithSamePhoneNumberFails() throws Exception {
        String phoneNumber = "0341" + (System.nanoTime() % 1_000_000);
        String email = "duplicate+" + System.nanoTime() + "@volako.mg";
        String payload = """
                {"firstName": "Nija", "lastName": "Rakoto", "phoneNumber": "%s", "email": "%s", "password": "SuperSecret123"}
                """.formatted(phoneNumber, email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("PHONE_NUMBER_TAKEN"));
    }
}
