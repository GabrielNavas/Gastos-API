package com.navas.gastosapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import java.util.Map;

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
class SqlInjectionTest extends AbstractIntegrationTest {

    private static final String[] PAYLOADS = {
            "ZAP AND 1=1 --",
            "ZAP AND 1=2 --",
            "x' OR '1'='1",
            "'; DROP TABLE categories; --"
    };

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private String nameBody(String name) throws Exception {
        return mapper.writeValueAsString(Map.of("name", name));
    }

    private long createCategory(String token, String name) throws Exception {
        String response = mvc.perform(post("/api/categories")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nameBody(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json(response).get("id").asLong();
    }

    @Test
    void payloadsDeSqlInjectionSaoGravadosComoTextoLiteral() throws Exception {
        String attacker = registerAndLogin();
        String bystander = registerAndLogin();
        long attackerCategory = createCategory(attacker, "original");
        createCategory(bystander, "intocada");

        for (String payload : PAYLOADS) {
            mvc.perform(put("/api/categories/" + attackerCategory)
                            .header("Authorization", bearer(attacker))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(nameBody(payload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(payload));
        }

        // A tabela continua existindo e o atacante so tem a propria categoria
        mvc.perform(get("/api/categories").header("Authorization", bearer(attacker)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value(PAYLOADS[PAYLOADS.length - 1]));

        // Os dados do outro usuario nao foram afetados
        mvc.perform(get("/api/categories").header("Authorization", bearer(bystander)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("intocada"));
    }

    @Test
    void parametroDeDataComPayloadERejeitadoComo400() throws Exception {
        String token = registerAndLogin();

        mvc.perform(get("/api/expenses")
                        .param("from", "2026-01-01' OR '1'='1")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }
}