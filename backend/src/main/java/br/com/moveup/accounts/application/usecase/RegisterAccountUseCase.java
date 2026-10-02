package br.com.moveup.accounts.application.usecase;

import br.com.moveup.accounts.application.port.in.RegisterAccount;
import br.com.moveup.accounts.application.port.out.AccountRepository;
import br.com.moveup.accounts.domain.exception.AccountAlreadyRegistered;
import br.com.moveup.accounts.domain.exception.InvalidAccountData;
import br.com.moveup.accounts.domain.model.Account;
import br.com.moveup.accounts.domain.model.AccountRole;
import br.com.moveup.accounts.domain.model.BirthDate;
import br.com.moveup.accounts.domain.model.Email;
import br.com.moveup.accounts.domain.model.PersonName;
import br.com.moveup.accounts.domain.model.ProfessionalSetup;
import br.com.moveup.billing.api.StartTrial;
import br.com.moveup.shared.domain.IdGenerator;
import java.time.Clock;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastro: aluno (com data de nascimento) ou profissional (com organização própria e período de
 * teste). O e-mail vem do provedor de login, nunca do corpo da requisição.
 */
public class RegisterAccountUseCase implements RegisterAccount {

  private final AccountRepository accounts;
  private final StartTrial startTrial;
  private final IdGenerator ids;
  private final Clock clock;

  public RegisterAccountUseCase(
      AccountRepository accounts, StartTrial startTrial, IdGenerator ids, Clock clock) {
    this.accounts = accounts;
    this.startTrial = startTrial;
    this.ids = ids;
    this.clock = clock;
  }

  @Override
  @Transactional
  public UUID handle(Command command) {
    if (command.tokenEmail() == null || command.tokenEmail().isBlank()) {
      throw new InvalidAccountData(
          "email-required", "Entre com uma conta que tenha e-mail (Google, Apple ou e-mail).");
    }
    var email = Email.of(command.tokenEmail());
    var name = PersonName.of(command.name());
    var role = parseRole(command.role());
    var today = Profiles.today(clock);

    if (accounts.existsByIdentityOrEmail(command.identity(), email)) {
      throw new AccountAlreadyRegistered();
    }

    var id = ids.newId();
    var account =
        switch (role) {
          case CLIENT ->
              Account.registerClient(
                  id, command.identity(), name, email, BirthDate.of(command.birthDate(), today));
          case PROFESSIONAL -> {
            var setup =
                ProfessionalSetup.of(command.businessName(), command.registryNumber(), name);
            yield command.birthDate() == null
                ? Account.registerProfessional(id, command.identity(), name, email, setup)
                : Account.registerProfessional(
                    id,
                    command.identity(),
                    name,
                    email,
                    setup,
                    BirthDate.of(command.birthDate(), today),
                    today);
          }
        };
    accounts.save(account);

    if (role == AccountRole.PROFESSIONAL) {
      var organizationId = ids.newId();
      accounts.createOrganization(organizationId, account);
      startTrial.forOrganization(organizationId);
    }
    return id;
  }

  private static AccountRole parseRole(String role) {
    try {
      return AccountRole.fromCode(role);
    } catch (IllegalArgumentException e) {
      throw new InvalidAccountData("role-invalid", "Escolha o perfil: profissional ou aluno.");
    }
  }
}
