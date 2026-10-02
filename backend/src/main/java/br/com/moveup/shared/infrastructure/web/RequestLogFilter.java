package br.com.moveup.shared.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Uma linha de log estruturado por requisição: método, rota, status e duração (o {@code traceId} e
 * o {@code userId} vêm do MDC). Registra a <em>rota padrão</em> ({@code
 * /v1/invites/{code}/accept}), nunca a URL real, que pode ter código de convite; e nunca corpo,
 * header ou query.
 */
@Component
public class RequestLogFilter extends OncePerRequestFilter {

  static final String UNMATCHED = "unmatched";

  private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return request.getRequestURI().startsWith("/actuator"); // health check a cada poucos segundos
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var start = System.nanoTime();
    try {
      chain.doFilter(request, response);
    } finally {
      var route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
      log.atInfo()
          .setMessage("http_request")
          .addKeyValue("method", request.getMethod())
          .addKeyValue("route", route instanceof String r ? r : UNMATCHED)
          .addKeyValue("status", response.getStatus())
          .addKeyValue("durationMs", (System.nanoTime() - start) / 1_000_000)
          .log();
    }
  }
}
