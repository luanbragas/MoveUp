package br.com.moveup.training.infrastructure.config;

import br.com.moveup.accounts.api.AccountDirectory;
import br.com.moveup.shared.domain.IdGenerator;
import br.com.moveup.training.application.port.in.ManageExercises;
import br.com.moveup.training.application.port.out.Exercises;
import br.com.moveup.training.application.usecase.ManageExercisesUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Monta os casos de uso do módulo training (adaptadores jOOQ são beans do pacote persistence). */
@Configuration(proxyBeanMethods = false)
class TrainingModuleConfig {

  @Bean
  ManageExercises manageExercises(AccountDirectory accounts, Exercises exercises, IdGenerator ids) {
    return new ManageExercisesUseCase(accounts, exercises, ids);
  }
}
