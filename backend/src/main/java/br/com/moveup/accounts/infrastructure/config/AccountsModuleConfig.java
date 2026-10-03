package br.com.moveup.accounts.infrastructure.config;

import br.com.moveup.accounts.api.AccountDirectory;
import br.com.moveup.accounts.application.port.in.GetMe;
import br.com.moveup.accounts.application.port.in.GuardianAuthorization;
import br.com.moveup.accounts.application.port.in.ManageConsents;
import br.com.moveup.accounts.application.port.in.ManageGuardianRequest;
import br.com.moveup.accounts.application.port.in.RegisterAccount;
import br.com.moveup.accounts.application.port.out.AccountReader;
import br.com.moveup.accounts.application.port.out.AccountRepository;
import br.com.moveup.accounts.application.port.out.ConsentRepository;
import br.com.moveup.accounts.application.port.out.GuardianConsentRepository;
import br.com.moveup.accounts.application.port.out.LegalDocuments;
import br.com.moveup.accounts.application.port.out.OrganizationReader;
import br.com.moveup.accounts.application.usecase.AccountDirectoryService;
import br.com.moveup.accounts.application.usecase.GetMeUseCase;
import br.com.moveup.accounts.application.usecase.GuardianAuthorizationUseCase;
import br.com.moveup.accounts.application.usecase.ManageConsentsUseCase;
import br.com.moveup.accounts.application.usecase.ManageGuardianRequestUseCase;
import br.com.moveup.accounts.application.usecase.RegisterAccountUseCase;
import br.com.moveup.billing.api.StartTrial;
import br.com.moveup.shared.domain.IdGenerator;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
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
  AccountDirectory accountDirectory(
      AccountReader accounts,
      OrganizationReader organizations,
      ConsentRepository consents,
      GuardianConsentRepository guardianConsents,
      LegalDocuments legalDocuments,
      Clock clock) {
    return new AccountDirectoryService(
        accounts, organizations, consents, guardianConsents, legalDocuments, clock);
  }

  @Bean
  ManageGuardianRequest manageGuardianRequest(
      AccountReader accounts,
      GuardianConsentRepository guardianConsents,
      LegalDocuments legalDocuments,
      IdGenerator ids,
      Clock clock,
      @Value("${moveup.guardian.link-ttl}") Duration linkTtl,
      @Value("${moveup.guardian.link-base-url}") String linkBaseUrl) {
    var random = new SecureRandom();
    return new ManageGuardianRequestUseCase(
        accounts,
        guardianConsents,
        legalDocuments,
        ids,
        random::nextBytes,
        clock,
        linkTtl,
        linkBaseUrl);
  }

  @Bean
  GuardianAuthorization guardianAuthorization(
      GuardianConsentRepository guardianConsents, LegalDocuments legalDocuments, Clock clock) {
    return new GuardianAuthorizationUseCase(guardianConsents, legalDocuments, clock);
  }
}
