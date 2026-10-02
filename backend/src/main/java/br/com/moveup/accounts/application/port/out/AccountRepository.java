package br.com.moveup.accounts.application.port.out;

import br.com.moveup.accounts.domain.model.Account;
import br.com.moveup.accounts.domain.model.Email;
import br.com.moveup.accounts.domain.model.LoginIdentity;
import java.util.UUID;

public interface AccountRepository {

  boolean existsByIdentityOrEmail(LoginIdentity identity, Email email);

  /** Grava a conta, a identidade do provedor e, se profissional, o perfil profissional. */
  void save(Account account);

  /** Organização do profissional (MVP: uma por profissional, ele como dono). */
  void createOrganization(UUID organizationId, Account owner);
}
