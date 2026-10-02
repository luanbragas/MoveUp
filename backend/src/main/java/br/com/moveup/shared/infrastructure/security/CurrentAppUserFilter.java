package br.com.moveup.shared.infrastructure.security;

import br.com.moveup.shared.infrastructure.web.RequestLogFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Depois da validação do JWT, resolve {@code sub} → {@code app_user.id} uma vez por requisição,
 * <em>antes</em> de qualquer transação de caso de uso (assim o {@code RlsTransactionManager} só lê
 * o valor, sem consultar o banco no meio da abertura da transação). Põe o id no MDC como {@code
 * userId}; quem limpa é o {@link RequestLogFilter}, depois de logar a requisição.
 *
 * <p>Não é um bean: entra só na cadeia do Spring Security (senão rodaria duas vezes).
 */
public class CurrentAppUserFilter extends OncePerRequestFilter {

  private final AppUserResolver resolver;

  public CurrentAppUserFilter(AppUserResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (SecurityContextHolder.getContext().getAuthentication()
        instanceof JwtAuthenticationToken t) {
      resolver
          .resolve(FirebaseJwt.PROVIDER, t.getToken().getSubject())
          .ifPresent(
              id -> {
                request.setAttribute(RequestCurrentAppUser.ATTRIBUTE, id);
                MDC.put(RequestLogFilter.MDC_USER_ID, id.toString());
              });
    }
    chain.doFilter(request, response);
  }
}
