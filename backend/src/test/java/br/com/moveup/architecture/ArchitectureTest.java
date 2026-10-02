package br.com.moveup.architecture;

import static br.com.moveup.architecture.ArchitectureRules.MODULES;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Aplica as regras de arquitetura ao código de produção (inclui o jOOQ gerado). */
@AnalyzeClasses(
    packages = ArchitectureTest.ROOT,
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  static final String ROOT = "br.com.moveup";

  @ArchTest static final ArchRule domainIsPure = ArchitectureRules.domainIsPure(ROOT);

  @ArchTest
  static final ArchRule applicationIgnoresInfrastructure =
      ArchitectureRules.applicationIgnoresInfrastructure(ROOT);

  @ArchTest
  static final ArchRule applicationOnlyUsesSpringTransactions =
      ArchitectureRules.applicationOnlyUsesSpringTransactions(ROOT);

  @ArchTest
  static final ArchRule jooqOnlyInPersistence = ArchitectureRules.jooqOnlyInPersistence(ROOT);

  @ArchTest
  static final ArchRule moduleBoundaries = ArchitectureRules.moduleBoundaries(ROOT, MODULES);

  @ArchTest
  static final ArchRule sharedIsIndependent = ArchitectureRules.sharedIsIndependent(ROOT, MODULES);
}
