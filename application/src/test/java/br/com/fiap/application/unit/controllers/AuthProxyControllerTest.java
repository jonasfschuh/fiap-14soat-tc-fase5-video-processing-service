package br.com.fiap.application.unit.controllers;

import br.com.fiap.application.adapters.AuthProxyController;
import br.com.fiap.application.dtos.AuthLoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@DisplayName("AuthProxyController - Unit Tests")
class AuthProxyControllerTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private AuthProxyController controller;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        controller = new AuthProxyController(restTemplate);
    }

    @Test
    @DisplayName("login without configured URL returns SERVICE_UNAVAILABLE")
    void login_withoutUrl_returnsServiceUnavailable() {
        AuthLoginRequest request = new AuthLoginRequest("admin", "admin123");

        ResponseEntity<Object> response = controller.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsKey("error");
    }

    @Test
    @DisplayName("login with configured URL and successful response returns OK")
    void login_withUrl_success_returnsLambdaResponse() {
        ReflectionTestUtils.setField(controller, "authLambdaUrl", "http://lambda.example.com");
        AuthLoginRequest request = new AuthLoginRequest("admin", "admin123");

        mockServer.expect(requestTo("http://lambda.example.com/auth/login"))
                .andRespond(withSuccess("{\"token\":\"jwt-token\"}", MediaType.APPLICATION_JSON));

        ResponseEntity<Object> response = controller.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        mockServer.verify();
    }

    @Test
    @DisplayName("login with HTTP 401 error returns same status")
    void login_withHttp401_returnsUnauthorized() {
        ReflectionTestUtils.setField(controller, "authLambdaUrl", "http://lambda.example.com");
        AuthLoginRequest request = new AuthLoginRequest("user", "wrong");

        mockServer.expect(requestTo("http://lambda.example.com/auth/login"))
                .andRespond(withUnauthorizedRequest());

        ResponseEntity<Object> response = controller.login(request);

        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        mockServer.verify();
    }

    @Test
    @DisplayName("login with connection failure returns 500")
    void login_withConnectionFailure_returns500() {
        ReflectionTestUtils.setField(controller, "authLambdaUrl", "http://lambda.example.com");
        AuthLoginRequest request = new AuthLoginRequest("admin", "admin123");

        mockServer.expect(requestTo("http://lambda.example.com/auth/login"))
                .andRespond(withException(new java.io.IOException("Connection refused")));

        ResponseEntity<Object> response = controller.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsKey("error");
        mockServer.verify();
    }

    @Test
    @DisplayName("AuthLoginRequest getters and setters work correctly")
    void authLoginRequest_gettersAndSetters() {
        AuthLoginRequest req = new AuthLoginRequest();
        req.setUsername("testuser");
        req.setPassword("testpass");

        assertThat(req.getUsername()).isEqualTo("testuser");
        assertThat(req.getPassword()).isEqualTo("testpass");
    }

    @Test
    @DisplayName("AuthLoginRequest two-arg constructor works correctly")
    void authLoginRequest_twoArgConstructor() {
        AuthLoginRequest req = new AuthLoginRequest("admin", "secret");

        assertThat(req.getUsername()).isEqualTo("admin");
        assertThat(req.getPassword()).isEqualTo("secret");
    }
}
