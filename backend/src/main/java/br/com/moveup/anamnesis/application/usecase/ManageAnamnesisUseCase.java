package br.com.moveup.anamnesis.application.usecase;

import br.com.moveup.anamnesis.application.port.in.ManageAnamnesis;
import br.com.moveup.anamnesis.application.port.out.AnamnesisStore;
import br.com.moveup.anamnesis.domain.exception.InvalidAnamnesisData;
import br.com.moveup.anamnesis.domain.model.Anamnesis;
import br.com.moveup.anamnesis.domain.model.AnamnesisTemplate;
import br.com.moveup.audit.api.AuditTrail;
import br.com.moveup.coaching.api.CoachingRoster;
import br.com.moveup.coaching.api.LinkDirectory;
import br.com.moveup.shared.domain.IdGenerator;
import br.com.moveup.shared.domain.ResourceNotFound;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class ManageAnamnesisUseCase implements ManageAnamnesis {

  public static final String SUBMITTED = "anamnesis.submitted";
  public static final String REVIEWED = "anamnesis.reviewed";

  private final AnamnesisStore store;
  private final CoachingRoster roster;
  private final LinkDirectory links;
  private final AuditTrail audit;
  private final IdGenerator ids;
  private final Clock clock;

  public ManageAnamnesisUseCase(
      AnamnesisStore store,
      CoachingRoster roster,
      LinkDirectory links,
      AuditTrail audit,
      IdGenerator ids,
      Clock clock) {
    this.store = store;
    this.roster = roster;
    this.links = links;
    this.audit = audit;
    this.ids = ids;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public AnamnesisTemplate template() {
    return store.currentTemplate();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<AnamnesisView> mine(UUID userId) {
    return roster
        .clientOfUser(userId)
        .flatMap(self -> store.latest(self.clientId()))
        .map(ManageAnamnesisUseCase::view);
  }

  @Override
  @Transactional
  public AnamnesisView submit(UUID userId, Map<String, Object> raw) {
    var self = roster.clientOfUser(userId).orElseThrow(ResourceNotFound::new);
    if (self.activeLinkId() == null) {
      throw new InvalidAnamnesisData(
          "anamnesis-without-link", "A anamnese é enviada depois de aceitar o convite.");
    }
    var template = store.currentTemplate();
    var answers = template.validate(raw);
    var latest = store.latest(self.clientId());
    Anamnesis saved;
    if (latest.isPresent() && !latest.get().reviewed()) {
      saved = latest.get().resubmitted(answers, userId);
      store.update(saved);
    } else {
      saved =
          Anamnesis.submitted(
              ids.newId(),
              self.clientId(),
              self.activeLinkId(),
              latest.map(a -> a.versionNumber() + 1).orElse(1),
              template,
              answers,
              userId,
              clock.instant());
      store.insert(saved);
    }
    store.announce(saved.id(), saved.clientId(), SUBMITTED);
    return view(saved);
  }

  @Override
  @Transactional
  public ClientAnamnesis ofLink(UUID professionalId, UUID linkId) {
    var link = links.ofProfessional(professionalId, linkId).orElseThrow(ResourceNotFound::new);
    var latest = store.latest(link.clientId());
    latest.ifPresent(a -> viewed(professionalId, a));
    return new ClientAnamnesis(
        latest.map(ManageAnamnesisUseCase::view), store.versions(link.clientId()));
  }

  @Override
  @Transactional
  public AnamnesisView version(UUID professionalId, UUID linkId, int versionNumber) {
    var link = links.ofProfessional(professionalId, linkId).orElseThrow(ResourceNotFound::new);
    var found = store.version(link.clientId(), versionNumber).orElseThrow(ResourceNotFound::new);
    viewed(professionalId, found);
    return view(found);
  }

  @Override
  @Transactional
  public AnamnesisView review(
      UUID professionalId,
      UUID linkId,
      Map<String, Object> raw,
      String clearance,
      LocalDate clearanceDate) {
    var link = links.ofProfessional(professionalId, linkId).orElseThrow(ResourceNotFound::new);
    if (!link.acceptsTraining()) {
      throw new ResourceNotFound(); // aluno inativo ou vínculo encerrado: só leitura
    }
    var latest = store.latest(link.clientId()).orElseThrow(ResourceNotFound::new);
    var template = store.currentTemplate();
    var answers = template.validate(raw);
    var decision =
        new Anamnesis.Clearance(Anamnesis.ClearanceStatus.fromCode(clearance), clearanceDate);
    var now = clock.instant();
    Anamnesis saved;
    if (latest.reviewed()) {
      // revisada é imutável: a mudança vira versão nova, já revisada pelo personal
      saved =
          Anamnesis.submitted(
                  ids.newId(),
                  link.clientId(),
                  link.linkId(),
                  latest.versionNumber() + 1,
                  template,
                  answers,
                  professionalId,
                  now)
              .reviewedBy(professionalId, answers, decision, now);
      store.insert(saved);
    } else {
      saved = latest.reviewedBy(professionalId, answers, decision, now);
      store.update(saved);
    }
    store.announce(saved.id(), saved.clientId(), REVIEWED);
    return view(saved);
  }

  private void viewed(UUID professionalId, Anamnesis anamnesis) {
    audit.record(
        new AuditTrail.Entry(
            professionalId,
            "anamnesis",
            anamnesis.id(),
            anamnesis.clientId(),
            AuditTrail.Action.VIEW_ANAMNESIS));
  }

  static AnamnesisView view(Anamnesis a) {
    return new AnamnesisView(
        a.versionNumber(),
        a.answers().values(),
        a.answers().parqPositive(),
        a.clearance().status().code(),
        a.clearance().date(),
        a.reviewed(),
        a.reviewedAt(),
        a.createdAt());
  }
}
