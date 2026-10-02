package br.com.moveup.accounts.infrastructure.config;

import br.com.moveup.accounts.application.port.in.DeclareGuardianConsent;
import br.com.moveup.accounts.application.port.in.GetMe;
import br.com.moveup.accounts.application.port.in.ManageConsents;
import br.com.moveup.accounts.application.port.in.RegisterAccount;
import br.com.moveup.accounts.application.port.out.AccountReader;
import br.com.moveup.accounts.application.port.out.AccountRepository;
import br.com.moveup.accounts.application.port.out.ConsentRepository;
import br.com.moveup.accounts.application.port.out.GuardianConsentRepository;
import br.com.moveup.accounts.application.port.out.LegalDocuments;
import br.com.moveup.accounts.application.usecase.DeclareGuardianConsentUseCase;
import br.com.moveup.accounts.application.usecase.GetMeUseCase;
import br.com.moveup.accounts.application.usecase.ManageConsentsUseCase;
import br.com.moveup.accounts.application.usecase.RegisterAccountUseCase;
import br.com.moveup.billing.api.StartTrial;
import br.com.moveup.shared.domain.IdGenerator;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Monta os casos de uso do módulo. Os adaptadores jOOQ são beans do próprio pacote {@code
 * persistence} (jOOQ não sai de lá, regra do ArchUnit).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LegalVersionsProperties.class)
class AccountsModuleConfig {

  @Bean
  GetMe getMe(
      AccountReader accounts,
      ConsentRepository consents,
      GuardianConsentRepository guardianConsents,
      LegalDocuments legalDocuments,
      Clock clock) {
    return new GetMeUseCase(accounts, consents, guardianConsents, legalDocuments, clock);
  }

  @Bean
  RegisterAccount registerAccount(
      AccountRepository accounts, StartTrial startTrial, IdGenerator ids, Clock clock) {
    return new RegisterAccountUseCase(accounts, startTrial, ids, clock);
  }

  @Bean
  ManageConsents manageConsents(ConsentRepository consents, LegalDocuments legalDocuments) {
    return new ManageConsentsUseCase(consents, legalDocuments);
  }

  @Bean
  DeclareGuardianConsent declareGuardianConsent(
      AccountReader accounts,
      GuardianConsentRepository guardianConsents,
      LegalDocuments legalDocuments,
      IdGenerator ids,
      Clock clock) {
    return new DeclareGuardianConsentUseCase(
        accounts, guardianConsents, legalDocuments, ids, clock);
  }
}
