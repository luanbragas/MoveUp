package br.com.moveup.shared.infrastructure.web;

import java.net.URI;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * Monta o {@code ProblemDetail} no formato único da API (BACKEND-PATTERN, seção 7): usado pelo
 * handler global e pelas respostas 401/403 da segurança, que acontecem fora do Spring MVC.
 */
@Component
public class Problems {

  public static final String TYPE_BASE = "https://api.moveup.com.br/problems/";

  private final TraceIds traceIds;

  public Problems(TraceIds traceIds) {
    this.traceIds = traceIds;
  }

  public ProblemDetail create(
      HttpStatusCode status, String code, String detail, @Nullable String path) {
    var pd = ProblemDetail.forStatusAndDetail(status, detail);
    pd.setType(URI.create(TYPE_BASE + code));
    pd.setTitle(Titles.of(code));
    if (path != null) {
      pd.setInstance(URI.create(path));
    }
    pd.setProperty("code", code);
    pd.setProperty("traceId", traceIds.current());
    return pd;
  }
}
