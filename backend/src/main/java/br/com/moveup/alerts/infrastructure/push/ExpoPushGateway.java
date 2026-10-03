package br.com.moveup.alerts.infrastructure.push;

import br.com.moveup.alerts.application.port.out.PushGateway;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Expo Push (https://docs.expo.dev/push-notifications/sending-notifications/). Um POST com o lote;
 * a resposta traz um resultado por mensagem. Nunca loga token nem texto.
 */
public class ExpoPushGateway implements PushGateway {

  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final Duration TIMEOUT = Duration.ofSeconds(15);

  private final HttpClient http;
  private final URI endpoint;

  public ExpoPushGateway(URI endpoint) {
    this.http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    this.endpoint = endpoint;
  }

  @Override
  public List<Result> send(List<Message> messages) {
    var body = new ArrayList<Map<String, Object>>();
    for (var m : messages) {
      var item = new LinkedHashMap<String, Object>();
      item.put("to", m.token());
      item.put("title", m.title());
      item.put("body", m.body());
      item.put("data", m.data());
      item.put("sound", "default");
      item.put("priority", "high");
      body.add(item);
    }
    HttpResponse<String> response;
    try {
      response =
          http.send(
              HttpRequest.newBuilder(endpoint)
                  .timeout(TIMEOUT)
                  .header("Content-Type", "application/json")
                  .header("Accept", "application/json")
                  .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)))
                  .build(),
              HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      throw new PushUnavailable(e.getClass().getSimpleName());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new PushUnavailable("interrupted");
    }
    var status = response.statusCode();
    if (status == 429 || status >= 500) {
      // o Expo recusou o lote inteiro sem processar: dá para tentar de novo
      return messages.stream().map(m -> new Result(false, false, true, "http-" + status)).toList();
    }
    if (status >= 400) {
      return messages.stream().map(m -> new Result(false, false, false, "http-" + status)).toList();
    }
    return parse(JSON.readTree(response.body()), messages.size());
  }

  static List<Result> parse(JsonNode root, int expected) {
    var data = root.path("data");
    var results = new ArrayList<Result>();
    for (var i = 0; i < expected; i++) {
      var item = data.path(i);
      if ("ok".equals(item.path("status").asString(""))) {
        results.add(new Result(true, false, false, null));
        continue;
      }
      var error = item.path("details").path("error").asString("unknown");
      results.add(
          new Result(
              false,
              "DeviceNotRegistered".equals(error),
              "MessageRateExceeded".equals(error),
              error));
    }
    return results;
  }
}
