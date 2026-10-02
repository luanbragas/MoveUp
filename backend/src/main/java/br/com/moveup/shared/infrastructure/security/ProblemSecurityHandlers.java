package br.com.moveup.shared.infrastructure.security;

import br.com.moveup.shared.infrastructure.web.Problems;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

/**
 * 401 ({@code unauthenticated}) e 403 ({@code forbidden}) no mesmo formato {@code ProblemDetail} do
 * resto da API. Os handlers padrão do resource server definem o status e o header {@code
 * WWW-Authenticate}; aqui só se escreve o corpo.
 */
class ProblemSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

  private final Problems problems;
  private final JsonMapper jsonMapper;
  private final BearerTokenAuthenticationEntryPoint bearerEntryPoint =
      new BearerTokenAuthenticationEntryPoint();
  private final BearerTokenAccessDeniedHandler bearerAccessDenied =
      new BearerTokenAccessDeniedHandler();

  ProblemSecurityHandlers(Problems problems, JsonMapper jsonMapper) {
    this.problems = problems;
    this.jsonMapper = jsonMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
      throws IOException {
    bearerEntryPoint.commence(request, response, ex);
    write(
        response, HttpStatus.UNAUTHORIZED, "unauthenticated", "Autenticação necessária.", request);
  }

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
      throws IOException {
    bearerAccessDenied.handle(request, response, ex);
    write(response, HttpStatus.FORBIDDEN, "forbidden", "Acesso negado.", request);
  }

  private void write(
      HttpServletResponse response,
      HttpStatus status,
      String code,
      String detail,
      HttpServletRequest request)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    jsonMapper.writeValue(
        response.getOutputStream(), problems.create(status, code, detail, request.getRequestURI()));
  }
}
