package com.navas.gastosapi.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "e-mail é obrigatório")
        @Size(max = 255, message = "e-mail muito longo")
        String email,

        @NotBlank(message = "senha é obrigatória")
        @Size(max = 72, message = "senha muito longa")
        String password) {
}
