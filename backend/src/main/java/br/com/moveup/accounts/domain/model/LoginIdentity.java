package br.com.moveup.accounts.domain.model;

/** Identidade no provedor de login: {@code provider} + {@code sub} do token. */
public record LoginIdentity(String provider, String subject) {

  public LoginIdentity {
    if (provider == null || provider.isBlank() || subject == null || subject.isBlank()) {
      throw new IllegalArgumentException("identidade do provedor incompleta");
    }
  }
}
