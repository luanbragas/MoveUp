package br.com.moveup.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

class RequestLogFilterTest {

  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
  private final Logger logger = (Logger) LoggerFactory.getLogger(RequestLogFilter.class);

  @BeforeEach
  void attach() {
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void detach() {
    logger.detachAppender(appender);
  }

  @Test
  void deveLogarARotaPadraoENuncaAUrlReal() throws Exception {
    // given
    var request = new MockHttpServletRequest("POST", "/v1/invites/SEGREDO123/accept");
    request.setQueryString("token=abc");
    var response = new MockHttpServletResponse();

    // when
    new RequestLogFilter()
        .doFilter(
            request,
            response,
            (req, res) -> {
              req.setAttribute(
                  HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/v1/invites/{code}/accept");
              ((MockHttpServletResponse) res).setStatus(409);
            });

    // then
    assertThat(appender.list).hasSize(1);
    var event = appender.list.getFirst();
    assertThat(event.getMessage()).isEqualTo("http_request");
    assertThat(keyValues(event))
        .containsEntry("method", "POST")
        .containsEntry("route", "/v1/invites/{code}/accept")
        .containsEntry("status", "409")
        .containsKey("durationMs");
    assertThat(event.toString() + keyValues(event)).doesNotContain("SEGREDO123", "token");
  }

  @Test
  void rotaSemHandlerViraUnmatched() throws Exception {
    new RequestLogFilter()
        .doFilter(
            new MockHttpServletRequest("GET", "/qualquer/coisa"),
            new MockHttpServletResponse(),
            (req, res) -> {});

    assertThat(keyValues(appender.list.getFirst())).containsEntry("route", "unmatched");
  }

  @Test
  void healthCheckNaoEhLogado() throws Exception {
    new RequestLogFilter()
        .doFilter(
            new MockHttpServletRequest("GET", "/actuator/health"),
            new MockHttpServletResponse(),
            (req, res) -> {});

    assertThat(appender.list).isEmpty();
  }

  private static Map<String, String> keyValues(ILoggingEvent event) {
    return event.getKeyValuePairs().stream()
        .collect(Collectors.toMap(kv -> kv.key, kv -> String.valueOf(kv.value)));
  }
}
