package br.com.moveup.accounts.infrastructure.web;

import br.com.moveup.accounts.application.port.in.RequestOrigin;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

/**
 * IP e app de quem aceitou. O IP vem de {@code getRemoteAddr()}: atrás do Cloudflare/ALB, o Spring
 * precisa de {@code server.forward-headers-strategy} configurado na infra (Fase 9).
 */
final class RequestOrigins {

  private RequestOrigins() {}

  static RequestOrigin of(HttpServletRequest request) {
    return new RequestOrigin(request.getRemoteAddr(), request.getHeader(HttpHeaders.USER_AGENT));
  }
}
