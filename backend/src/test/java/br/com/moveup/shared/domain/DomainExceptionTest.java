package br.com.moveup.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DomainExceptionTest {

  static final class InvalidPrescription extends DomainException {
    InvalidPrescription(String code) {
      super(code, "Faixa de repetições inválida.");
    }
  }

  @Test
  void deveGuardarCodeEMensagemSegura() {
    var ex = new InvalidPrescription("reps-range-invalid");

    assertThat(ex.code()).isEqualTo("reps-range-invalid");
    assertThat(ex.getMessage()).isEqualTo("Faixa de repetições inválida.");
  }

  @Test
  void codeForaDeKebabCaseEhRejeitado() {
    for (var bad : new String[] {"RepsInvalid", "reps_invalid", "reps-", "", "reps invalid"}) {
      assertThatThrownBy(() -> new InvalidPrescription(bad))
          .as(bad)
          .isInstanceOf(IllegalArgumentException.class);
    }
    assertThatThrownBy(() -> new InvalidPrescription(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void recursoNaoEncontradoTemCodeFixo() {
    assertThat(new ResourceNotFound().code()).isEqualTo("resource-not-found");
  }
}
