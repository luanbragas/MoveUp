package br.com.moveup.accounts.application.port.in;

import br.com.moveup.accounts.domain.model.LoginIdentity;
import java.time.LocalDate;
import java.util.UUID;

/** Cria a conta do usuário logado no provedor (SCREEN-FLOWS 0.2). Devolve o id interno. */
public interface RegisterAccount {

  UUID handle(Command command);

  /**
   * @param tokenEmail e-mail vindo do token do provedor (nulo se o provedor não deu)
   * @param role {@code professional} ou {@code client}
   * @param birthDate obrigatória para aluno; opcional para profissional
   * @param businessName só profissional; vazio = nome da pessoa
   * @param registryNumber só profissional; CREF opcional
   */
  record Command(
      LoginIdentity identity,
      String tokenEmail,
      String name,
      String role,
      LocalDate birthDate,
      String businessName,
      String registryNumber) {}
}
