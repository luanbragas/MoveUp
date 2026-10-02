package br.com.moveup.accounts.infrastructure.config;

import br.com.moveup.accounts.application.port.in.GetMe;
import br.com.moveup.accounts.application.port.out.AccountReader;
import br.com.moveup.accounts.application.usecase.GetMeUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Monta os casos de uso do módulo. Os adaptadores jOOQ são beans do próprio pacote {@code
 * persistence} (jOOQ não sai de lá, regra do ArchUnit).
 */
@Configuration(proxyBeanMethods = false)
class AccountsModuleConfig {

  @Bean
  GetMe getMe(AccountReader accounts) {
    return new GetMeUseCase(accounts);
  }
}
