package br.com.moveup.shared.infrastructure.web;

import br.com.moveup.shared.domain.ConflictException;
import br.com.moveup.shared.domain.DomainException;
import br.com.moveup.shared.domain.Forbidden;
import br.com.moveup.shared.domain.ResourceNotFound;
import br.com.moveup.shared.domain.VersionMismatch;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Toda resposta de erro em {@code application/problem+json} (RFC 9457), com {@code code} estável e
 * {@code traceId} (BACKEND-PATTERN, seção 7). Nunca expõe stack trace, SQL, classe Java, valor
 * enviado ou dado de saúde: o {@code detail} é sempre um texto do catálogo ou da exceção de
 * domínio.
 */
@RestControllerAdvice
public class GlobalProblemHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalProblemHandler.class);

  /** Erros do próprio Spring MVC, por status. */
  private static final Map<Integer, String> FRAMEWORK_CODES =
      Map.of(
          400, "malformed-request",
          404, "resource-not-found",
          405, "method-not-allowed",
          406, "not-acceptable",
          413, "payload-too-large",
          415, "unsupported-media-type");

  private final Problems problems;

  public GlobalProblemHandler(Problems problems) {
    this.problems = problems;
  }

  @ExceptionHandler(ResourceNotFound.class)
  ProblemDetail notFound(ResourceNotFound ex, HttpServletRequest request) {
    return problem(HttpStatus.NOT_FOUND, ex.code(), ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(Forbidden.class)
  ProblemDetail forbidden(Forbidden ex, HttpServletRequest request) {
    return problem(HttpStatus.FORBIDDEN, ex.code(), ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(VersionMismatch.class)
  ProblemDetail versionMismatch(VersionMismatch ex, HttpServletRequest request) {
    return problem(
        ex.missing() ? HttpStatus.PRECONDITION_REQUIRED : HttpStatus.PRECONDITION_FAILED,
        ex.code(),
        ex.getMessage(),
        request.getRequestURI());
  }

  @ExceptionHandler(ConflictException.class)
  ProblemDetail conflict(ConflictException ex, HttpServletRequest request) {
    return problem(HttpStatus.CONFLICT, ex.code(), ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(DomainException.class)
  ProblemDetail domain(DomainException ex, HttpServletRequest request) {
    return problem(
        HttpStatus.UNPROCESSABLE_ENTITY, ex.code(), ex.getMessage(), request.getRequestURI());
  }

  @ExceptionHandler(DataAccessException.class)
  ProblemDetail database(DataAccessException ex, HttpServletRequest request) {
    return fromDatabase(ex, request);
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail unexpected(Exception ex, HttpServletRequest request) {
    // Erro do banco que não veio como DataAccessException do Spring (ex.: exceção do próprio
    // jOOQ ao chamar função): mesmo tratamento, sem logar a mensagem (tem SQL e valores).
    if (sqlState(ex) != null) {
      return fromDatabase(ex, request);
    }
    log.error("unexpected_error", ex); // sem payload da requisição
    return internalError(request.getRequestURI());
  }

  private ProblemDetail fromDatabase(Exception ex, HttpServletRequest request) {
    var translated = PostgresErrorTranslator.translate(ex);
    if (translated.isPresent()) {
      var t = translated.get();
      return problem(t.status(), t.code(), Titles.of(t.code()) + ".", request.getRequestURI());
    }
    // A mensagem do banco pode trazer valores (ex.: "Key (email)=(...)"): só SQLSTATE e tipo.
    log.error("database_error sqlState={} type={}", sqlState(ex), ex.getClass().getSimpleName());
    return internalError(request.getRequestURI());
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    var errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> fieldError(e.getField(), e))
            .toList();
    return validationFailed(errors, headers, request);
  }

  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    var errors =
        ex.getParameterValidationResults().stream()
            .flatMap(
                result ->
                    result.getResolvableErrors().stream()
                        .map(e -> fieldError(result.getMethodParameter().getParameterName(), e)))
            .toList();
    return validationFailed(errors, headers, request);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    // A mensagem do Jackson pode ecoar o conteúdo enviado: não vai para a resposta nem para o log.
    var pd =
        problem(
            HttpStatus.BAD_REQUEST,
            "malformed-request",
            "O corpo da requisição não pôde ser lido.",
            path(request));
    return ResponseEntity.badRequest().headers(headers).body(pd);
  }

  /** Demais erros do Spring MVC (404 de rota, 405, 415...): mesmo formato, texto do catálogo. */
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex,
      @Nullable Object body,
      HttpHeaders headers,
      HttpStatusCode statusCode,
      WebRequest request) {
    var code =
        statusCode.is5xxServerError()
            ? "internal-error"
            : FRAMEWORK_CODES.getOrDefault(statusCode.value(), "malformed-request");
    var pd = problem(statusCode, code, Titles.of(code) + ".", path(request));
    return ResponseEntity.status(statusCode).headers(headers).body(pd);
  }

  // ---------------------------------------------------------------------------------------------

  private ResponseEntity<Object> validationFailed(
      List<Map<String, String>> errors, HttpHeaders headers, WebRequest request) {
    var pd =
        problem(
            HttpStatus.BAD_REQUEST,
            "validation-failed",
            "Um ou mais campos são inválidos.",
            path(request));
    pd.setProperty("errors", errors);
    return ResponseEntity.badRequest().headers(headers).body(pd);
  }

  /** Campo, código da restrição (ex.: {@code not-null}) e mensagem. Nunca o valor enviado. */
  private static Map<String, String> fieldError(String field, MessageSourceResolvable error) {
    var codes = error.getCodes();
    var constraint = codes == null || codes.length == 0 ? "invalid" : last(codes);
    var message = error.getDefaultMessage();
    return Map.of(
        "field", field == null ? "" : field,
        "code", kebab(constraint),
        "message", message == null ? "" : message);
  }

  /** Para {@link FieldError} o último código é o nome curto da restrição (ex.: NotNull). */
  private static String last(String[] codes) {
    var last = codes[codes.length - 1];
    var dot = last.lastIndexOf('.');
    return dot < 0 ? last : last.substring(dot + 1);
  }

  static String kebab(String name) {
    return name.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase();
  }

  private ProblemDetail internalError(String path) {
    return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Erro inesperado.", path);
  }

  private ProblemDetail problem(HttpStatusCode status, String code, String detail, String path) {
    return problems.create(status, code, detail, path);
  }

  @Nullable
  private static String path(WebRequest request) {
    if (request instanceof NativeWebRequest nativeRequest
        && nativeRequest.getNativeRequest() instanceof HttpServletRequest servlet) {
      return servlet.getRequestURI();
    }
    return null;
  }

  @Nullable
  private static String sqlState(Throwable error) {
    for (var cause = error; cause != null; cause = cause.getCause()) {
      if (cause instanceof SQLException sql) {
        return sql.getSQLState();
      }
    }
    return null;
  }
}
