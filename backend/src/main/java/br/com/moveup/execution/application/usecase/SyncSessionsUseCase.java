package br.com.moveup.execution.application.usecase;

import br.com.moveup.accounts.api.AccountDirectory;
import br.com.moveup.coaching.api.LinkDirectory;
import br.com.moveup.execution.application.port.in.SyncSessions;
import br.com.moveup.execution.application.port.out.Sessions;
import br.com.moveup.execution.domain.exception.InvalidSessionData;
import br.com.moveup.execution.domain.model.PerformedSession;
import br.com.moveup.execution.domain.model.PerformedSession.PerformedBy;
import br.com.moveup.execution.domain.model.SyncOutcome;
import br.com.moveup.shared.domain.ResourceNotFound;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class SyncSessionsUseCase implements SyncSessions {

  private final AccountDirectory accounts;
  private final LinkDirectory links;
  private final Sessions sessions;

  public SyncSessionsUseCase(AccountDirectory accounts, LinkDirectory links, Sessions sessions) {
    this.accounts = accounts;
    this.links = links;
    this.sessions = sessions;
  }

  @Override
  @Transactional
  public SyncResult push(UUID userId, List<PerformedSession> incoming) {
    if (incoming.size() > MAX_SESSIONS) {
      throw new InvalidSessionData("sync-too-big", "Envie no máximo 50 sessões por vez.");
    }
    var professional = accounts.organizationOf(userId).isPresent();
    var written = new ArrayList<UUID>();
    var unchanged = new ArrayList<UUID>();
    var by = professional ? PerformedBy.PROFESSIONAL : PerformedBy.CLIENT;
    for (var received : incoming) {
      var session = received.registeredBy(by);
      var link =
          (professional
                  ? links.ofProfessional(userId, session.linkId())
                  : links.ofClientUser(userId, session.linkId()))
              .orElseThrow(ResourceNotFound::new);
      if (professional && !link.acceptsTraining()) {
        throw new ResourceNotFound(); // presencial só com aluno pendente ou ativo
      }
      var outcome = SyncOutcome.decide(session, sessions.lockStored(session.id()).orElse(null));
      if (!outcome.write()) {
        unchanged.add(session.id());
        continue;
      }
      sessions.write(session, link.clientId(), userId, outcome.editedAfterFinish());
      if (outcome.justFinished()) {
        sessions.announceFinished(session.id(), link.clientId());
      }
      written.add(session.id());
    }
    return new SyncResult(written, unchanged);
  }
}
