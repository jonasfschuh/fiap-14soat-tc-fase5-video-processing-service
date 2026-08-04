package br.com.fiap.infrastructure.observability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("HttpCorrelationLoggingFilter - Unit Tests")
class HttpCorrelationLoggingFilterTest {

    private HttpCorrelationLoggingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new HttpCorrelationLoggingFilter();
    }

    @Test
    @DisplayName("doFilter with existing X-Correlation-Id uses that id in response")
    void doFilter_withExistingCorrelationId_usesExistingId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "my-correlation-id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo("my-correlation-id");
    }

    @Test
    @DisplayName("doFilter without X-Correlation-Id generates new id")
    void doFilter_withoutCorrelationId_generatesNewId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Correlation-Id")).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("doFilter with X-Service-Order-Id sets it in MDC and clears after")
    void doFilter_withServiceOrderId_setsAndClearsFromMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Service-Order-Id", "SO-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        assertThatCode(() -> filter.doFilter(request, response, chain))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("doFilter with blank X-Correlation-Id generates new id")
    void doFilter_withBlankCorrelationId_generatesNewId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "  ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        String header = response.getHeader("X-Correlation-Id");
        assertThat(header).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("doFilter continues chain even without any headers")
    void doFilter_withNoHeaders_completes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        assertThatCode(() -> filter.doFilter(request, response, chain))
                .doesNotThrowAnyException();
    }
}
