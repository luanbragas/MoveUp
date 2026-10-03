package br.com.moveup.coaching.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.moveup.accounts.api.AccountDirectory;
import br.com.moveup.audit.api.AuditTrail;
import br.com.moveup.billing.api.PlanLimits;
import br.com.moveup.coaching.application.port.in.AnswerInvite.InvitePreview;
import br.com.moveup.coaching.application.port.in.InviteClient.NewClient;
import br.com.moveup.coaching.application.port.in.ListClients;
import br.com.moveup.coaching.application.port.in.ListClients.ClientItem;
import br.com.moveup.coaching.application.port.in.ListClients.ClientPage;
import br.com.moveup.coaching.application.port.out.CoachingLinks;
import br.com.moveup.coaching.application.port.out.CoachingLinks.ClientLink;
import br.com.moveup.coaching.application.port.out.Invites;
import br.com.moveup.coaching.application.usecase.AnswerInviteUseCase;
import br.com.moveup.coaching.application.usecase.InvitationIssuer;
import br.com.moveup.coaching.application.usecase.InviteClientUseCase;
import br.com.moveup.coaching.application.usecase.ListClientsUseCase;
import br.com.moveup.coaching.application.usecase.ManageLinkUseCase;
import br.com.moveup.coaching.domain.exception.CoachingConflict;
import br.com.moveup.coaching.domain.model.ClientPreRegistration;
import br.com.moveup.coaching.domain.model.CoachingLink;
import br.com.moveup.coaching.domain.model.InviteCode;
import br.com.moveup.coaching.domain.model.LinkStatus;
import br.com.moveup.shared.domain.DomainException;
import br.com.moveup.shared.domain.Forbidden;
import br.com.moveup.shared.domain.ResourceNotFound;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Casos de uso do vínculo com fakes em memória, inclusive os caminhos de autorização. */
class CoachingUseCasesTest {

  static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-02T12:00:00Z"), ZoneOffset.UTC);
  static final UUID PRO = UUID.randomUUID();
  static final UUID OTHER_PRO = UUID.randomUUID();
  static final UUID ORG = UUID.randomUUID();
  static final UUID STUDENT = UUID.randomUUID();

  // ---------------------------------------------------------------- fakes

  final Map<UUID, CoachingLink> links = new LinkedHashMap<>();
  final Map<UUID, String> codes = new HashMap<>(); // link -> código pendente
  final List<AuditTrail.Entry> audit = new ArrayList<>();
  final Map<UUID, AccountDirectory.LinkReadiness> readiness = new HashMap<>();
  int limit = 2;

  final CoachingLinks linkRepo =
      new CoachingLinks() {
        @Override
        public Optional<CoachingLink> find(UUID linkId) {
          return Optional.ofNullable(links.get(linkId));
        }

        @Override
        public void createPending(
            UUID clientId, UUID linkId, UUID org, UUID pro, ClientPreRegistration client) {
          links.put(linkId, new CoachingLink(linkId, org, pro, clientId, LinkStatus.PENDING, null));
        }

        @Override
        public void saveStatus(CoachingLink link) {
          links.put(link.id(), link);
        }

        @Override
        public int countActive(UUID org) {
          return (int)
              links.values().stream()
                  .filter(l -> l.organizationId().equals(org))
                  .filter(l -> l.status() == LinkStatus.ACTIVE)
                  .count();
        }

        @Override
        public ClientPage list(UUID pro, UUID cursor, int pageSize) {
          var items =
              links.values().stream()
                  .filter(l -> l.belongsTo(pro))
                  .map(
                      l ->
                          new ClientItem(
                              l.id(), l.clientId(), "Aluno", l.status().code(), null, null))
                  .toList();
          return new ClientPage(items, null);
        }

        @Override
        public List<ClientLink> ofClientUser(UUID userId) {
          return List.of();
        }
      };

  final Invites inviteRepo =
      new Invites() {
        @Override
        public void issue(UUID linkId, InviteCode code, Instant expiresAt) {
          codes.put(linkId, code.value());
        }

        @Override
        public int revokePending(UUID linkId) {
          return codes.remove(linkId) == null ? 0 : 1;
        }

        @Override
        public Optional<InvitePreview> preview(InviteCode code) {
          return codes.containsValue(code.value())
              ? Optional.of(new InvitePreview("Ana", "Studio", CLOCK.instant()))
              : Optional.empty();
        }

        @Override
        public UUID accept(InviteCode code) {
          var linkId =
              codes.entrySet().stream()
                  .filter(e -> e.getValue().equals(code.value()))
                  .map(Map.Entry::getKey)
                  .findFirst()
                  .orElseThrow(CoachingConflict::inviteExpired);
          var pending = links.get(linkId);
          links.put(
              linkId,
              new CoachingLink(
                  linkId,
                  pending.organizationId(),
                  pending.professionalId(),
                  pending.clientId(),
                  LinkStatus.ACTIVE,
                  null));
          codes.remove(linkId);
          return linkId;
        }

        @Override
        public void endAsClient(UUID linkId) {
          throw new UnsupportedOperationException("coberto no teste de ponta a ponta");
        }

        @Override
        public InviteCode newCode() {
          return InviteCode.generate(new java.util.Random()::nextInt);
        }
      };

  final AccountDirectory accounts =
      new AccountDirectory() {
        @Override
        public Optional<UUID> organizationOf(UUID user) {
          return user.equals(PRO) ? Optional.of(ORG) : Optional.empty();
        }

        @Override
        public LinkReadiness linkReadiness(UUID user) {
          return readiness.getOrDefault(user, LinkReadiness.NOT_A_CLIENT);
        }

        @Override
        public Optional<ProfessionalCard> professionalCard(UUID user) {
          return Optional.of(new ProfessionalCard("Ana", "Studio"));
        }
      };

  final PlanLimits planLimits =
      new PlanLimits() {
        @Override
        public OptionalInt activeClientLimit(UUID org) {
          return OptionalInt.of(limit);
        }

        @Override
        public OptionalInt lockActiveClientLimit(UUID org) {
          return OptionalInt.of(limit);
        }
      };

  final InvitationIssuer issuer =
      new InvitationIssuer(inviteRepo, CLOCK, Duration.ofDays(7), "https://moveup.test/i");
  final InviteClientUseCase inviteClient =
      new InviteClientUseCase(accounts, planLimits, linkRepo, issuer, UUID::randomUUID);
  final AnswerInviteUseCase answer = new AnswerInviteUseCase(accounts, inviteRepo);
  final ManageLinkUseCase manage =
      new ManageLinkUseCase(linkRepo, inviteRepo, issuer, planLimits, audit::add, CLOCK);
  final ListClientsUseCase list = new ListClientsUseCase(linkRepo, accounts, planLimits);

  static String codeOf(Throwable error) {
    return ((DomainException) error).code();
  }

  // ---------------------------------------------------------------- testes

  @Test
  void convitePendenteComLinkEValidadeDeSeteDias() {
    var invitation = inviteClient.handle(PRO, new NewClient("Bia", null, null, null));

    assertThat(invitation.url()).isEqualTo("https://moveup.test/i/" + invitation.code());
    assertThat(invitation.expiresAt()).isEqualTo(CLOCK.instant().plus(Duration.ofDays(7)));
    assertThat(links.get(invitation.linkId()).status()).isEqualTo(LinkStatus.PENDING);
    assertThat(list.handle(PRO, null, 0).items()).hasSize(1);
  }

  @Test
  void quemNaoEhProfissionalNaoConvida() {
    assertThatThrownBy(() -> inviteClient.handle(STUDENT, new NewClient("Bia", null, null, null)))
        .isInstanceOf(Forbidden.class);
  }

  @Test
  void planoSemVagaNaoConvida() {
    assertThat(list.seats(PRO)).isEqualTo(new ListClients.Seats(0, 2));
    limit = 0;

    assertThatThrownBy(() -> inviteClient.handle(PRO, new NewClient("Bia", null, null, null)))
        .satisfies(e -> assertThat(codeOf(e)).isEqualTo(CoachingConflict.PLAN_LIMIT_REACHED));
  }

  @Test
  void soAlunoComOnboardingCompletoAceita() {
    var invitation = inviteClient.handle(PRO, new NewClient("Bia", null, null, null));

    assertThatThrownBy(() -> answer.accept(STUDENT, invitation.code()))
        .satisfies(e -> assertThat(codeOf(e)).isEqualTo(CoachingConflict.INVITE_FOR_CLIENTS_ONLY));
    readiness.put(STUDENT, AccountDirectory.LinkReadiness.ONBOARDING_INCOMPLETE);
    assertThatThrownBy(() -> answer.accept(STUDENT, invitation.code()))
        .satisfies(e -> assertThat(codeOf(e)).isEqualTo(CoachingConflict.ONBOARDING_INCOMPLETE));

    readiness.put(STUDENT, AccountDirectory.LinkReadiness.READY);
    assertThat(answer.preview(invitation.code()).professionalName()).isEqualTo("Ana");
    assertThat(answer.accept(STUDENT, invitation.code().toLowerCase()))
        .isEqualTo(invitation.linkId());
    assertThat(links.get(invitation.linkId()).status()).isEqualTo(LinkStatus.ACTIVE);
  }

  @Test
  void codigoMalFormadoOuDesconhecidoEhConviteExpirado() {
    readiness.put(STUDENT, AccountDirectory.LinkReadiness.READY);

    assertThatThrownBy(() -> answer.preview("???"))
        .satisfies(e -> assertThat(codeOf(e)).isEqualTo(CoachingConflict.INVITE_EXPIRED));
    assertThatThrownBy(() -> answer.preview("ABCDEFGH"))
        .satisfies(e -> assertThat(codeOf(e)).isEqualTo(CoachingConflict.INVITE_EXPIRED));
    assertThatThrownBy(() -> answer.accept(STUDENT, "x"))
        .satisfies(e -> assertThat(codeOf(e)).isEqualTo(CoachingConflict.INVITE_EXPIRED));
  }

  @Test
  void reenviarTrocaOCodigoECancelarApaga() {
    var first = inviteClient.handle(PRO, new NewClient("Bia", null, null, null));

    var second = manage.resendInvite(PRO, first.linkId());
    assertThat(second.code()).isNotEqualTo(first.code());
    assertThat(codes.get(first.linkId())).isEqualTo(second.code());

    manage.cancelInvite(PRO, first.linkId());
    assertThat(codes).doesNotContainKey(first.linkId());
  }

  @Test
  void cicloDeVidaComTrilhaDeAuditoria() {
    var linkId = activeLink();

    manage.inactivate(PRO, linkId);
    manage.reactivate(PRO, linkId);
    manage.end(PRO, linkId);

    assertThat(links.get(linkId).status()).isEqualTo(LinkStatus.ENDED);
    assertThat(audit)
        .extracting(AuditTrail.Entry::action)
        .containsExactly(
            AuditTrail.Action.LINK_CHANGED,
            AuditTrail.Action.LINK_CHANGED,
            AuditTrail.Action.LINK_ENDED);
    assertThat(audit).allMatch(e -> e.actorId().equals(PRO) && e.entityId().equals(linkId));
  }

  @Test
  void reativarSemVagaEhBarrado() {
    var linkId = activeLink();
    manage.inactivate(PRO, linkId);
    activeLink();
    activeLink(); // ocupa as 2 vagas

    assertThatThrownBy(() -> manage.reactivate(PRO, linkId))
        .satisfies(e -> assertThat(codeOf(e)).isEqualTo(CoachingConflict.PLAN_LIMIT_REACHED));
  }

  @Test
  void outroProfissionalNaoMexeNoVinculo_404() {
    var linkId = activeLink();

    assertThatThrownBy(() -> manage.inactivate(OTHER_PRO, linkId))
        .isInstanceOf(ResourceNotFound.class);
    assertThatThrownBy(() -> manage.end(OTHER_PRO, linkId)).isInstanceOf(ResourceNotFound.class);
    assertThatThrownBy(() -> manage.resendInvite(OTHER_PRO, linkId))
        .isInstanceOf(ResourceNotFound.class);
    assertThat(links.get(linkId).status()).isEqualTo(LinkStatus.ACTIVE);
  }

  @Test
  void vinculoEncerradoNaoVoltaNemRecebeConvite() {
    var linkId = activeLink();
    manage.end(PRO, linkId);

    assertThatThrownBy(() -> manage.reactivate(PRO, linkId))
        .satisfies(e -> assertThat(codeOf(e)).isEqualTo(CoachingConflict.LINK_STATE_INVALID));
    assertThatThrownBy(() -> manage.resendInvite(PRO, linkId))
        .satisfies(e -> assertThat(codeOf(e)).isEqualTo(CoachingConflict.LINK_STATE_INVALID));
  }

  private UUID activeLink() {
    var id = UUID.randomUUID();
    links.put(id, new CoachingLink(id, ORG, PRO, UUID.randomUUID(), LinkStatus.ACTIVE, null));
    return id;
  }
}
