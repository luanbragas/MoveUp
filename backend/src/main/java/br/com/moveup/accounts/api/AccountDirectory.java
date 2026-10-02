package br.com.moveup.accounts.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Porta pública do módulo accounts para os outros módulos (ex.: coaching): papel, organização e
 * prontidão do onboarding, sem expor as tabelas de contas.
 */
public interface AccountDirectory {

  /** Organização do profissional (MVP: uma por profissional); vazio se não é profissional. */
  Optional<UUID> organizationOf(UUID professionalUserId);

  /** Se a conta pode entrar num vínculo como aluno agora. */
  LinkReadiness linkReadiness(UUID userId);

  /** Nome do profissional e do negócio, para o aluno saber com quem está. */
  Optional<ProfessionalCard> professionalCard(UUID professionalUserId);

  record ProfessionalCard(String name, String organizationName) {}

  enum LinkReadiness {
    /** Aluno com aceites em dia e, se menor, com autorização do responsável. */
    READY,
    /** Conta não é de aluno (ex.: profissional) ou não existe. */
    NOT_A_CLIENT,
    /** Aluno com aceite pendente ou, se menor, sem o responsável. */
    ONBOARDING_INCOMPLETE
  }
}
