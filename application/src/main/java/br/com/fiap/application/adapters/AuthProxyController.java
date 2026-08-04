package br.com.fiap.application.adapters;

import br.com.fiap.application.dtos.AuthLoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Login — obtenha o JWT token para usar os endpoints protegidos")
public class AuthProxyController {

    private static final Logger log = LoggerFactory.getLogger(AuthProxyController.class);

    @Value("${auth.lambda.url:}")
    private String authLambdaUrl;

    private final RestTemplate restTemplate;

    public AuthProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostMapping("/login")
    @Operation(
        summary = "Login (obter JWT token)",
        description = """
                Autentica o usuario e retorna o JWT token.

                **Passos:**
                1. Preencha `username` e `password`
                2. Clique em **Execute**
                3. Copie o valor `token` da resposta
                4. Clique em **Authorize** (🔒) e informe: `Bearer <token>`

                **Credenciais de teste:**
                - username: `admin` / password: `admin123` (ADMIN)
                - username: `user` / password: `user123` (USER)
                """
    )
    public ResponseEntity<Object> login(@RequestBody AuthLoginRequest request) {
        if (!StringUtils.hasText(authLambdaUrl)) {
            log.warn("AUTH_LAMBDA_URL nao configurado — login proxy indisponivel");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "AUTH_LAMBDA_URL nao configurado. Adicione o secret no K8s."));
        }
        try {
            ResponseEntity<Object> response = restTemplate.postForEntity(
                    authLambdaUrl + "/auth/login",
                    request,
                    Object.class
            );
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.warn("Erro ao chamar auth lambda: {} {}", e.getStatusCode(), e.getMessage());
            return ResponseEntity.status(e.getStatusCode())
                    .body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Erro inesperado ao chamar auth lambda", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro ao conectar com o servico de autenticacao: " + e.getMessage()));
        }
    }
}
