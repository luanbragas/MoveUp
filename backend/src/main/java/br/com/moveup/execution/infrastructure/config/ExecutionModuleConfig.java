package br.com.moveup.execution.infrastructure.config;

import br.com.moveup.accounts.api.AccountDirectory;
import br.com.moveup.coaching.api.LinkDirectory;
import br.com.moveup.execution.application.port.in.SyncSessions;
import br.com.moveup.execution.application.port.out.Sessions;
import br.com.moveup.execution.application.usecase.SyncSessionsUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Monta os casos de uso do módulo execution (adaptadores jOOQ são beans do pacote persistence). */
@Configuration(proxyBeanMethods = false)
class ExecutionModuleConfig {

  @Bean
  SyncSessions syncSessions(AccountDirectory accounts, LinkDirectory links, Sessions sessions) {
    return new SyncSessionsUseCase(accounts, links, sessions);
  }
}
