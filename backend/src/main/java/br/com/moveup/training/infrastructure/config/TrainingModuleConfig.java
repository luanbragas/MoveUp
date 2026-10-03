package br.com.moveup.training.infrastructure.config;

import br.com.moveup.accounts.api.AccountDirectory;
import br.com.moveup.coaching.api.LinkDirectory;
import br.com.moveup.execution.api.PlannedVersionUsage;
import br.com.moveup.shared.domain.IdGenerator;
import br.com.moveup.training.application.port.in.ManageExercises;
import br.com.moveup.training.application.port.in.ManagePrograms;
import br.com.moveup.training.application.port.in.ManageWorkouts;
import br.com.moveup.training.application.port.in.PlannedSync;
import br.com.moveup.training.application.port.out.ExerciseCatalog;
import br.com.moveup.training.application.port.out.Exercises;
import br.com.moveup.training.application.port.out.Programs;
import br.com.moveup.training.application.port.out.Workouts;
import br.com.moveup.training.application.usecase.ManageExercisesUseCase;
import br.com.moveup.training.application.usecase.ManageProgramsUseCase;
import br.com.moveup.training.application.usecase.ManageWorkoutsUseCase;
import br.com.moveup.training.application.usecase.PlannedSyncUseCase;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Monta os casos de uso do módulo training (adaptadores jOOQ são beans do pacote persistence). */
@Configuration(proxyBeanMethods = false)
class TrainingModuleConfig {

  @Bean
  ManageExercises manageExercises(AccountDirectory accounts, Exercises exercises, IdGenerator ids) {
    return new ManageExercisesUseCase(accounts, exercises, ids);
  }

  @Bean
  ManageWorkouts manageWorkouts(
      AccountDirectory accounts,
      Workouts workouts,
      ExerciseCatalog catalog,
      PlannedVersionUsage versionUsage,
      IdGenerator ids) {
    return new ManageWorkoutsUseCase(accounts, workouts, catalog, versionUsage, ids);
  }

  @Bean
  ManagePrograms managePrograms(
      LinkDirectory links,
      Programs programs,
      Workouts workouts,
      ExerciseCatalog catalog,
      IdGenerator ids) {
    return new ManageProgramsUseCase(links, programs, workouts, catalog, ids);
  }

  @Bean
  PlannedSync plannedSync(
      AccountDirectory accounts,
      Programs programs,
      Workouts workouts,
      Exercises exercises,
      Clock clock) {
    return new PlannedSyncUseCase(accounts, programs, workouts, exercises, clock);
  }
}
