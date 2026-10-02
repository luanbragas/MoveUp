package br.com.moveup.accounts.application.port.in;

import java.util.UUID;

/** Consentimento de um dos pais ou do responsável pelo aluno menor (LGPD, art. 14). */
public interface DeclareGuardianConsent {

  void handle(Command command);

  record Command(
      UUID userId,
      String guardianName,
      String guardianEmail,
      String relationship,
      String docVersion,
      RequestOrigin origin) {}
}
