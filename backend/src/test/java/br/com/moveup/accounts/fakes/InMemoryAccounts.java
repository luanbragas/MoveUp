package br.com.moveup.accounts.fakes;

import br.com.moveup.accounts.application.port.out.AccountReader;
import br.com.moveup.accounts.application.port.out.AccountRepository;
import br.com.moveup.accounts.domain.model.Account;
import br.com.moveup.accounts.domain.model.BirthDate;
import br.com.moveup.accounts.domain.model.Email;
import br.com.moveup.accounts.domain.model.LoginIdentity;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Contas em memória: faz o papel do repositório (escrita) e do leitor (leitura). */
public final class InMemoryAccounts implements AccountRepository, AccountReader {

  public final Map<UUID, Account> accounts = new LinkedHashMap<>();
  public final Map<UUID, UUID> organizationsByOwner = new LinkedHashMap<>();

  @Override
  public boolean existsByIdentityOrEmail(LoginIdentity identity, Email email) {
    return accounts.values().stream()
        .anyMatch(a -> a.identity().equals(identity) || a.email().equals(email));
  }

  @Override
  public void save(Account account) {
    accounts.put(account.id(), account);
  }

  @Override
  public void createOrganization(UUID organizationId, Account owner) {
    organizationsByOwner.put(owner.id(), organizationId);
  }

  @Override
  public Optional<AccountSummary> find(UUID userId) {
    return Optional.ofNullable(accounts.get(userId))
        .map(
            a ->
                new AccountSummary(
                    a.id(),
                    a.name().value(),
                    a.email().value(),
                    "pt-BR",
                    "America/Sao_Paulo",
                    "kg",
                    "cm",
                    a.role().code(),
                    a.birthDate().map(BirthDate::value).orElse(null)));
  }
}
