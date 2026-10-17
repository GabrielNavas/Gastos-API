package com.navas.gastosapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "jwt.secret=segredo-de-teste-com-mais-de-32-caracteres-123",
        "rate-limit.auth-per-minute=1000",
        "rate-limit.general-per-minute=1000"
})
@AutoConfigureMockMvc
class ExpenseApiTest extends AbstractIntegrationTest {

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String expenseJson(String amount, String date, Long categoryId) {
        return "{\"amount\":" + amount + ",\"description\":\"Almoço\",\"spentOn\":\"" + date + "\""
                + (categoryId != null ? ",\"categoryId\":" + categoryId : "") + "}";
    }

    private long createCategory(String token, String name) throws Exception {
        String response = mvc.perform(post("/api/categories")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json(response).get("id").asLong();
    }

    private long createExpense(String token, String amount, String date, Long categoryId) throws Exception {
        String response = mvc.perform(post("/api/expenses")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson(amount, date, categoryId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json(response).get("id").asLong();
    }

    @Test
    void crudDeDespesas() throws Exception {
        String token = registerAndLogin();
        long categoryId = createCategory(token, "Alimentação");
        long expenseId = createExpense(token, "45.90", "2026-10-01", categoryId);

        mvc.perform(get("/api/expenses/" + expenseId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(45.90))
                .andExpect(jsonPath("$.categoryId").value(categoryId));

        mvc.perform(put("/api/expenses/" + expenseId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("50.00", "2026-10-02", categoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(50.00));

        mvc.perform(get("/api/expenses").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(expenseId));

        mvc.perform(delete("/api/expenses/" + expenseId).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/expenses/" + expenseId).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void filtraPorPeriodo() throws Exception {
        String token = registerAndLogin();
        createExpense(token, "10.00", "2026-01-10", null);
        createExpense(token, "20.00", "2026-06-10", null);

        mvc.perform(get("/api/expenses")
                        .param("from", "2026-05-01").param("to", "2026-12-31")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void usuarioNaoAcessaDespesaDeOutroUsuario() throws Exception {
        String tokenA = registerAndLogin();
        String tokenB = registerAndLogin();
        long expenseId = createExpense(tokenA, "99.90", "2026-10-01", null);

        mvc.perform(get("/api/expenses/" + expenseId).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/expenses/" + expenseId)
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("1.00", "2026-10-01", null)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/expenses/" + expenseId).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());

        // O dono continua com a despesa intacta
        mvc.perform(get("/api/expenses/" + expenseId).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk());
    }

    @Test
    void naoPermiteUsarCategoriaDeOutroUsuario() throws Exception {
        String tokenA = registerAndLogin();
        String tokenB = registerAndLogin();
        long categoryOfA = createCategory(tokenA, "Privada");

        mvc.perform(post("/api/expenses")
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("10.00", "2026-10-01", categoryOfA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void validaEntradaDeDespesa() throws Exception {
        String token = registerAndLogin();

        mvc.perform(post("/api/expenses")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("-5", "2026-10-01", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.amount").exists());

        mvc.perform(post("/api/expenses")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.spentOn").exists());
    }

    @Test
    void categoriaDuplicadaRetorna409() throws Exception {
        String token = registerAndLogin();
        createCategory(token, "Transporte");
        mvc.perform(post("/api/categories")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Transporte\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void rotasProtegidasExigemToken() throws Exception {
        mvc.perform(get("/api/expenses")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/categories")).andExpect(status().isUnauthorized());
    }
}
