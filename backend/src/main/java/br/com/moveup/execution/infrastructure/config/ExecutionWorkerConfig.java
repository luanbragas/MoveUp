package br.com.moveup.execution.infrastructure.config;

import br.com.moveup.execution.application.port.out.RecordHistory;
import br.com.moveup.execution.application.usecase.RecalculateRecordsUseCase;
import br.com.moveup.execution.infrastructure.messaging.SessionRecordsHandler;
import br.com.moveup.shared.application.outbox.OutboxHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Peças do execution que só o worker usa: recordes refeitos a cada sessão finalizada/corrigida. */
@Configuration(proxyBeanMethods = false)
@Profile("worker")
class ExecutionWorkerConfig {

  @Bean
  OutboxHandler sessionRecordsHandler(RecordHistory history) {
    return new SessionRecordsHandler(new RecalculateRecordsUseCase(history));
  }
}
