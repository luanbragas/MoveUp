package br.com.moveup.architecture.fixtures.training.domain;

/** Domínio limpo: não viola nada. */
public class Workout {}

/** Violação: domínio usando Spring. */
class SpringAwareWorkout {
  void check(Object value) {
    org.springframework.util.Assert.notNull(value, "obrigatório");
  }
}
