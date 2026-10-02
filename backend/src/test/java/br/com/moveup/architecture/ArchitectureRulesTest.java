package br.com.moveup.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Prova que cada regra de {@link ArchitectureRules} pega a violação e deixa passar o permitido,
 * usando as classes de exemplo em {@code fixtures} (só existem nos testes).
 */
class ArchitectureRulesTest {

  private static final String ROOT = "br.com.moveup.architecture.fixtures";
  private static final List<String> MODULES = List.of("training", "execution");
  private static final JavaClasses FIXTURES = new ClassFileImporter().importPackages(ROOT);

  @Test
  void dominioNaoPodeUsarSpring() {
    var violations = violations(ArchitectureRules.domainIsPure(ROOT));

    assertThat(violations).anyMatch(v -> v.contains("SpringAwareWorkout"));
    assertThat(violations).noneMatch(v -> v.contains("training.domain.Workout>"));
  }

  @Test
  void casoDeUsoNaoPodeUsarAdaptador() {
    var violations = violations(ArchitectureRules.applicationIgnoresInfrastructure(ROOT));

    assertThat(violations).anyMatch(v -> v.contains("LeakyUseCase"));
  }

  @Test
  void casoDeUsoSoPodeUsarTransacaoDoSpring() {
    var violations = violations(ArchitectureRules.applicationOnlyUsesSpringTransactions(ROOT));

    assertThat(violations).anyMatch(v -> v.contains("AnnotatedUseCase"));
    assertThat(violations).noneMatch(v -> v.contains("TransactionalUseCase"));
  }

  @Test
  void jooqSoNoAdaptadorDePersistencia() {
    var violations = violations(ArchitectureRules.jooqOnlyInPersistence(ROOT));

    assertThat(violations).anyMatch(v -> v.contains("JooqInController"));
    assertThat(violations).noneMatch(v -> v.contains("WorkoutRepository"));
  }

  @Test
  void outroModuloSoEntraPelaApi() {
    var violations = violations(ArchitectureRules.moduleBoundaries(ROOT, MODULES));

    assertThat(violations).anyMatch(v -> v.contains("ReachesIntoTrainingDomain"));
    assertThat(violations).noneMatch(v -> v.contains("UsesTrainingApi"));
    assertThat(violations).noneMatch(v -> v.contains("LeakyUseCase"));
  }

  @Test
  void sharedNaoConheceModulos() {
    var violations = violations(ArchitectureRules.sharedIsIndependent(ROOT, MODULES));

    assertThat(violations).anyMatch(v -> v.contains("KnowsTraining"));
  }

  private static List<String> violations(ArchRule rule) {
    return rule.evaluate(FIXTURES).getFailureReport().getDetails();
  }
}
