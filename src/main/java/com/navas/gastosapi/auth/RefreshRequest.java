package com.navas.gastosapi.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshRequest(
        @NotBlank(message = "refreshToken é obrigatório")
        @Size(max = 200, message = "refreshToken inválido")
        String refreshToken) {
}
