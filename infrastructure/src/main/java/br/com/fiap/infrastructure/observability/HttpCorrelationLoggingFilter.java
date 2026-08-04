package br.com.fiap.infrastructure.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Reads X-Correlation-Id and X-Service-Order-Id from incoming HTTP requests
 * and places them in the MDC so all log statements in this MS automatically
 * include these fields — enabling distributed tracing in New Relic.
 *
 * If X-Correlation-Id is absent (direct calls to this MS), a new UUID is generated.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HttpCorrelationLoggingFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER   = "X-Correlation-Id";
    private static final String SERVICE_ORDER_ID_HEADER = "X-Service-Order-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId  = resolveCorrelationId(request);
        String serviceOrderId = request.getHeader(SERVICE_ORDER_ID_HEADER);

        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        MDC.put("correlationId", correlationId);
        if (serviceOrderId != null && !serviceOrderId.isBlank()) {
            MDC.put("serviceOrderId", serviceOrderId.trim());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
            MDC.remove("serviceOrderId");
        }
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String id = request.getHeader(CORRELATION_ID_HEADER);
        return (id != null && !id.isBlank()) ? id.trim() : UUID.randomUUID().toString();
    }
}
