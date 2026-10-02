package br.com.moveup.accounts.application.port.in;

/** De onde veio o aceite (IP e app), guardado junto do consentimento como prova (LGPD). */
public record RequestOrigin(String ip, String userAgent) {

  private static final int MAX_USER_AGENT = 500;

  public RequestOrigin {
    if (userAgent != null && userAgent.length() > MAX_USER_AGENT) {
      userAgent = userAgent.substring(0, MAX_USER_AGENT);
    }
  }
}
