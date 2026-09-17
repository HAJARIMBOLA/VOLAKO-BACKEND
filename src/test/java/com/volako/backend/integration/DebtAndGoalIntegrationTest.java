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
class DebtAndGoalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;

    @BeforeEach
    void registerUser() throws Exception {
        String phoneNumber = "0343" + (System.nanoTime() % 1_000_000);
        String email = "debts+" + System.nanoTime() + "@volako.mg";
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
    void receivableDebtPaymentCreditsTheChosenAccount() throws Exception {
        long accountId = createAccount("Cash");

        String debtBody = mockMvc.perform(post("/api/debts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"direction": "RECEIVABLE", "personName": "Jean", "amount": 100000, "dueDate": "%s"}
                                """.formatted(LocalDate.now().minusDays(1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.overdue").value(true))
                .andReturn().getResponse().getContentAsString();
        long debtId = objectMapper.readTree(debtBody).get("id").asLong();

        // Overdue endpoint surfaces it (drives the "someone still owes me" alert).
        mockMvc.perform(get("/api/debts/overdue").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].personName").value("Jean"));

        // Jean pays back half: account is credited, debt becomes partially paid.
        mockMvc.perform(post("/api/debts/" + debtId + "/payments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId": %d, "amount": 50000, "paymentDate": "%s"}
                                """.formatted(accountId, LocalDate.now())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].balance").value(50000));

        mockMvc.perform(get("/api/debts").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$[0].remainingAmount").value(50000));
    }

    @Test
    void payableDebtPaymentIsRejectedWhenBalanceInsufficient() throws Exception {
        long accountId = createAccountWithoutNegativeBalance("Cash");

        String debtBody = mockMvc.perform(post("/api/debts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"direction": "PAYABLE", "personName": "Marc", "amount": 150000}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long debtId = objectMapper.readTree(debtBody).get("id").asLong();

        mockMvc.perform(post("/api/debts/" + debtId + "/payments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId": %d, "amount": 10000, "paymentDate": "%s"}
                                """.formatted(accountId, LocalDate.now())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_BALANCE"));
    }

    @Test
    void goalTracksContributionsIndependentlyOfAccountBalance() throws Exception {
        long accountId = createAccount("Cash");

        String goalBody = mockMvc.perform(post("/api/goals")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "MacBook", "targetAmount": 4000000}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.progressPercent").value(0))
                .andReturn().getResponse().getContentAsString();
        long goalId = objectMapper.readTree(goalBody).get("id").asLong();

        mockMvc.perform(post("/api/goals/" + goalId + "/contributions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId": %d, "amount": 1500000, "contributionDate": "%s"}
                                """.formatted(accountId, LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.savedAmount").value(1500000))
                .andExpect(jsonPath("$.progressPercent").value(37));

        // Contributions are a separate ledger: the account's real balance is untouched.
        mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].balance").value(0));
    }

    private long createAccount(String name) throws Exception {
        String body = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "type": "CASH", "allowNegativeBalance": true}
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
