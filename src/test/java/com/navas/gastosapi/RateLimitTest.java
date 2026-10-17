package com.navas.gastosapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "jwt.secret=segredo-de-teste-com-mais-de-32-caracteres-123",
        "rate-limit.auth-per-minute=3",
        "rate-limit.general-per-minute=3"
})
@AutoConfigureMockMvc
class RateLimitTest extends AbstractIntegrationTest {

    @Test
    void bloqueiaTentativasDeLoginAcimaDoLimite() throws Exception {
        String body = credentials(uniqueEmail(), "qualquer-senha-123");

        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void limitePorUsuarioNasRotasProtegidas() throws Exception {
        // Os 3 primeiros requests da API geral (por IP, sem token) passam; o 4o e bloqueado
        for (int i = 0; i < 3; i++) {
            mvc.perform(get("/api/expenses")).andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/expenses")).andExpect(status().isTooManyRequests());
    }
}
