package br.com.moveup.coaching.application.port.in;

import java.time.Instant;
import java.util.UUID;

/** Profissional pré-cadastra o aluno e gera o convite (SCREEN-FLOWS 2.2). */
public interface InviteClient {

  Invitation handle(UUID professionalId, NewClient client);

  record NewClient(String name, String email, String phone, String goal) {}

  /** O que o profissional compartilha: código, link e validade. */
  record Invitation(UUID clientId, UUID linkId, String code, String url, Instant expiresAt) {}
}
