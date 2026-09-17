package com.volako.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
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
class AccountTransactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;

    @BeforeEach
    void registerUser() throws Exception {
        String phoneNumber = "0342" + (System.nanoTime() % 1_000_000);
        String payload = """
                {"phoneNumber": "%s", "password": "SuperSecret123", "fullName": "Nija Rakoto"}
                """.formatted(phoneNumber);

        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        accessToken = objectMapper.readTree(body).get("accessToken").asText();
    }

    @Test
    void createAccountThenTransactionsAffectBalance() throws Exception {
        long accountId = createAccount("Cash", "CASH", false);
        long expenseCategoryId = findCategoryId("Nourriture", "EXPENSE");
        long incomeCategoryId = findCategoryId("Salaire", "INCOME");

        // A negative-balance-disallowed account starts at 0: any expense is rejected.
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionPayload("EXPENSE", "10000", accountId, expenseCategoryId, "Trop tôt")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_BALANCE"));

        // Add income first.
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionPayload("INCOME", "100000", accountId, incomeCategoryId, "Salaire")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].balance").value(100000));

        // Expense larger than balance is still rejected.
        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionPayload("EXPENSE", "150000", accountId, expenseCategoryId, "Trop cher")))
                .andExpect(status().isConflict());

        // A reasonable expense succeeds and updates the balance.
        String transactionBody = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionPayload("EXPENSE", "30000", accountId, expenseCategoryId, "Courses")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long transactionId = objectMapper.readTree(transactionBody).get("id").asLong();

        mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].balance").value(70000));

        // Deleting the expense restores the balance.
        mockMvc.perform(delete("/api/transactions/" + transactionId).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].balance").value(100000));

        // Archiving the account never physically deletes it.
        mockMvc.perform(delete("/api/accounts/" + accountId).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(false));
    }

    @Test
    void dashboardReturnsZeroTotalsForABrandNewUser() throws Exception {
        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBalance").value(0))
                .andExpect(jsonPath("$.totalIncome").value(0))
                .andExpect(jsonPath("$.totalExpense").value(0));
    }

    @Test
    void dashboardSumsIncomeAndExpenseForTheCurrentPeriod() throws Exception {
        long accountId = createAccount("Cash", "CASH", true);
        long expenseCategoryId = findCategoryId("Nourriture", "EXPENSE");
        long incomeCategoryId = findCategoryId("Salaire", "INCOME");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionPayload("INCOME", "200000", accountId, incomeCategoryId, "Salaire")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionPayload("EXPENSE", "45000", accountId, expenseCategoryId, "Courses")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBalance").value(155000))
                .andExpect(jsonPath("$.totalIncome").value(200000))
                .andExpect(jsonPath("$.totalExpense").value(45000));
    }

    private long createAccount(String name, String type, boolean allowNegativeBalance) throws Exception {
        String payload = """
                {"name": "%s", "type": "%s", "allowNegativeBalance": %s}
                """.formatted(name, type, allowNegativeBalance);

        String body = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).get("id").asLong();
    }

    private long findCategoryId(String name, String type) throws Exception {
        String body = mockMvc.perform(get("/api/categories").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        for (JsonNode node : objectMapper.readTree(body)) {
            if (node.get("name").asText().equals(name) && node.get("type").asText().equals(type)) {
                return node.get("id").asLong();
            }
        }
        throw new IllegalStateException("Catégorie système introuvable: " + name);
    }

    private String transactionPayload(String type, String amount, long accountId, long categoryId, String description) {
        return """
                {"type": "%s", "amount": %s, "accountId": %d, "categoryId": %d, "description": "%s", "transactionDate": "%s"}
                """.formatted(type, amount, accountId, categoryId, description, LocalDate.now());
    }
}
