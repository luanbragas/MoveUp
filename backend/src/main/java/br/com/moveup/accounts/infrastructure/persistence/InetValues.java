package br.com.moveup.accounts.infrastructure.persistence;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;
import org.jooq.Field;
import org.jooq.impl.DSL;

/**
 * IP para colunas {@code inet}: só grava endereço IP literal válido (nunca um nome de host vindo de
 * header). O valor vai por bind; o fragmento só faz o cast do tipo.
 */
final class InetValues {

  private static final Pattern IP_CHARS = Pattern.compile("[0-9a-fA-F:.]{2,45}");

  private InetValues() {}

  static Field<String> inet(String ip) {
    return DSL.field(
        "cast({0} as inet)", String.class, DSL.val(isIpLiteral(ip) ? ip : null, String.class));
  }

  static boolean isIpLiteral(String ip) {
    if (ip == null || !IP_CHARS.matcher(ip).matches()) {
      return false;
    }
    try {
      // só dígitos hex, ':' e '.': é literal, então não há consulta de DNS
      InetAddress.getByName(ip);
      return true;
    } catch (UnknownHostException e) {
      return false;
    }
  }
}
