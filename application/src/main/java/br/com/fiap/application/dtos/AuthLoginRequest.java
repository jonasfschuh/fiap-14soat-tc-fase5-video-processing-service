package br.com.fiap.application.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class AuthLoginRequest {

    @NotBlank(message = "Username e obrigatorio")
    @Schema(description = "Username ou CPF do usuario", example = "admin")
    private String username;

    @NotBlank(message = "Senha e obrigatoria")
    @Schema(description = "Senha do usuario", example = "admin123")
    private String password;

    public AuthLoginRequest() {}

    public AuthLoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
