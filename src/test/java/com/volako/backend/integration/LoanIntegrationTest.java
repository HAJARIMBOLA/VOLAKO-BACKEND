package com.volako.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoanIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;

    @BeforeEach
    void registerUser() throws Exception {
        String phoneNumber = "0344" + (System.nanoTime() % 1_000_000);
        String email = "loans+" + System.nanoTime() + "@volako.mg";
        String payload = """
                {"firstName": "Nija", "lastName": "Rakoto", "phoneNumber": "%s", "email": "%s", "password": "SuperSecret123"}
                """.formatted(phoneNumber, email);

        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        accessToken = objectMapper.readTree(body).get("accessToken").asText();
    }

    @Test
    void loanPaymentDebitsAccountAndTracksInstallments() throws Exception {
        long accountId = createAccount("BNI");

        String loanBody = mockMvc.perform(post("/api/loans")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Prêt auto", "principalAmount": 300000, "monthlyPayment": 100000, "durationMonths": 3, "startDate": "%s"}
                                """.formatted(LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paidInstallments").value(0))
                .andExpect(jsonPath("$.remainingAmount").value(300000))
                .andExpect(jsonPath("$.completed").value(false))
                .andReturn().getResponse().getContentAsString();
        long loanId = objectMapper.readTree(loanBody).get("id").asLong();

        mockMvc.perform(post("/api/loans/" + loanId + "/payments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId": %d, "amount": 100000, "paymentDate": "%s"}
                                """.formatted(accountId, LocalDate.now())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].balance").value(-100000));

        mockMvc.perform(get("/api/loans").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paidInstallments").value(1))
                .andExpect(jsonPath("$[0].remainingAmount").value(200000))
                .andExpect(jsonPath("$[0].completed").value(false));
    }

    @Test
    void loanPaymentIsRejectedWhenBalanceInsufficient() throws Exception {
        long accountId = createAccountWithoutNegativeBalance("Cash");

        String loanBody = mockMvc.perform(post("/api/loans")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Prêt perso", "principalAmount": 200000, "monthlyPayment": 50000, "durationMonths": 4, "startDate": "%s"}
                                """.formatted(LocalDate.now())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long loanId = objectMapper.readTree(loanBody).get("id").asLong();

        mockMvc.perform(post("/api/loans/" + loanId + "/payments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId": %d, "amount": 50000, "paymentDate": "%s"}
                                """.formatted(accountId, LocalDate.now())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_BALANCE"));
    }

    private long createAccount(String name) throws Exception {
        String body = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "type": "BANK", "allowNegativeBalance": true}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private long createAccountWithoutNegativeBalance(String name) throws Exception {
        String body = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "type": "CASH", "allowNegativeBalance": false}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }
}
