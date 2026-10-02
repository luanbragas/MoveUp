package br.com.moveup.shared.infrastructure.web;

import io.micrometer.tracing.Tracer;
import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * {@code traceId} da requisição atual (o mesmo que o Micrometer Tracing põe no MDC dos logs). Sem
 * span ativo, gera um id aleatório: o {@code ProblemDetail} sempre tem {@code traceId}.
 */
@Component
public class TraceIds {

  private static final SecureRandom RANDOM = new SecureRandom();

  private final ObjectProvider<Tracer> tracer;

  public TraceIds(ObjectProvider<Tracer> tracer) {
    this.tracer = tracer;
  }

  public String current() {
    var active = tracer.getIfAvailable();
    var span = active == null ? null : active.currentSpan();
    var traceId = span == null ? null : span.context().traceId();
    // tracer no-op (ex.: testes, tracing desligado) devolve id vazio ou só zeros
    if (traceId != null && !traceId.isBlank() && !traceId.chars().allMatch(c -> c == '0')) {
      return traceId;
    }
    var bytes = new byte[16];
    RANDOM.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }
}
