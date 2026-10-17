package com.navas.gastosapi.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "nome é obrigatório")
        @Size(max = 80, message = "nome deve ter no máximo 80 caracteres")
        String name) {
}
