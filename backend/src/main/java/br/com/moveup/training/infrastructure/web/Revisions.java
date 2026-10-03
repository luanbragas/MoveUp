package br.com.moveup.training.infrastructure.web;

import br.com.moveup.shared.domain.VersionMismatch;
import java.util.regex.Pattern;

/** ETag {@code "r<revision>"} dos recursos editáveis (treino, programa) e a leitura do If-Match. */
final class Revisions {

  private static final Pattern ETAG = Pattern.compile("^(?:W/)?\"r(\\d{1,9})\"$");

  private Revisions() {}

  static String etag(int revision) {
    return "\"r" + revision + "\"";
  }

  /** Sem If-Match = 428; formato estranho = 412 (como uma revisão que não existe). */
  static int fromIfMatch(String ifMatch) {
    if (ifMatch == null || ifMatch.isBlank()) {
      throw VersionMismatch.required();
    }
    var matcher = ETAG.matcher(ifMatch.strip());
    if (!matcher.matches()) {
      throw VersionMismatch.stale();
    }
    return Integer.parseInt(matcher.group(1));
  }
}
