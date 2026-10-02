package br.com.moveup.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import java.util.List;

/**
 * Regras de camadas e de fronteira de módulo (BACKEND-PATTERN, seções 2, 3 e 12).
 *
 * <p>Recebem o pacote raiz para poderem ser exercitadas contra classes de exemplo com violações
 * ({@code ArchitectureRulesTest}): enquanto os módulos estão vazios, as regras passariam sem checar
 * nada ({@code allowEmptyShould}), então é esse teste que prova que elas funcionam.
 */
final class ArchitectureRules {

  /** Módulos de negócio. {@code shared} é o núcleo compartilhado e fica fora desta lista. */
  static final List<String> MODULES =
      List.of(
          "accounts",
          "coaching",
          "anamnesis",
          "training",
          "execution",
          "assessment",
          "alerts",
          "billing",
          "audit");

  private ArchitectureRules() {}

  static ArchRule domainIsPure(String root) {
    return noClasses()
        .that()
        .resideInAPackage(root + "..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "org.jooq..",
            "jakarta..",
            "com.fasterxml..",
            "tools.jackson..",
            "..application..",
            "..infrastructure..")
        .because("o domínio é Java puro")
        .allowEmptyShould(true);
  }

  static ArchRule applicationIgnoresInfrastructure(String root) {
    return noClasses()
        .that()
        .resideInAPackage(root + "..application..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..infrastructure..")
        .because("casos de uso dependem de portas, não de adaptadores")
        .allowEmptyShould(true);
  }

  /** Casos de uso são classes Java puras (sem {@code @Service}); só {@code @Transactional}. */
  static ArchRule applicationOnlyUsesSpringTransactions(String root) {
    return noClasses()
        .that()
        .resideInAPackage(root + "..application..")
        .should()
        .dependOnClassesThat(
            resideInAPackage("org.springframework..")
                .and(not(resideInAPackage("org.springframework.transaction.."))))
        .because("os beans são montados em infrastructure/config")
        .allowEmptyShould(true);
  }

  static ArchRule jooqOnlyInPersistence(String root) {
    return noClasses()
        .that()
        .resideInAPackage(root + "..")
        .and()
        .resideOutsideOfPackage("..infrastructure.persistence..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("org.jooq..")
        .because("jOOQ é detalhe do adaptador de persistência")
        .allowEmptyShould(true);
  }

  /** Outro módulo só enxerga o pacote {@code api}; domain, application e infrastructure não. */
  static ArchRule moduleBoundaries(String root, List<String> modules) {
    var rules =
        modules.stream()
            .map(
                module -> {
                  var base = root + "." + module;
                  return (ArchRule)
                      noClasses()
                          .that()
                          .resideOutsideOfPackage(base + "..")
                          .should()
                          .dependOnClassesThat()
                          .resideInAnyPackage(
                              base + ".domain..",
                              base + ".application..",
                              base + ".infrastructure..")
                          .because("o módulo " + module + " só é acessível pelo pacote api")
                          .allowEmptyShould(true);
                })
            .toList();
    return CompositeArchRule.of(rules);
  }

  /** O núcleo compartilhado não conhece nenhum módulo (senão vira dependência circular). */
  static ArchRule sharedIsIndependent(String root, List<String> modules) {
    return noClasses()
        .that()
        .resideInAPackage(root + ".shared..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(modules.stream().map(m -> root + "." + m + "..").toArray(String[]::new))
        .because("shared é a base de todos os módulos")
        .allowEmptyShould(true);
  }
}
