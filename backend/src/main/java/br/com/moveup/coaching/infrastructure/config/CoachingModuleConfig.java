package br.com.moveup.coaching.infrastructure.config;

import br.com.moveup.accounts.api.AccountDirectory;
import br.com.moveup.audit.api.AuditTrail;
import br.com.moveup.billing.api.PlanLimits;
import br.com.moveup.coaching.application.port.in.AnswerInvite;
import br.com.moveup.coaching.application.port.in.InviteClient;
import br.com.moveup.coaching.application.port.in.ListClients;
import br.com.moveup.coaching.application.port.in.ManageLink;
import br.com.moveup.coaching.application.port.out.CoachingLinks;
import br.com.moveup.coaching.application.port.out.Invites;
import br.com.moveup.coaching.application.usecase.AnswerInviteUseCase;
import br.com.moveup.coaching.application.usecase.InvitationIssuer;
import br.com.moveup.coaching.application.usecase.InviteClientUseCase;
import br.com.moveup.coaching.application.usecase.ListClientsUseCase;
import br.com.moveup.coaching.application.usecase.ManageLinkUseCase;
import br.com.moveup.shared.domain.IdGenerator;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class CoachingModuleConfig {

  @Bean
  InvitationIssuer invitationIssuer(
      Invites invites,
      Clock clock,
      @Value("${moveup.invite.ttl:P7D}") Duration ttl,
      @Value("${moveup.invite.link-base-url}") String linkBaseUrl) {
    return new InvitationIssuer(invites, clock, ttl, linkBaseUrl);
  }

  @Bean
  InviteClient inviteClient(
      AccountDirectory accounts,
      PlanLimits planLimits,
      CoachingLinks links,
      InvitationIssuer issuer,
      IdGenerator ids) {
    return new InviteClientUseCase(accounts, planLimits, links, issuer, ids);
  }

  @Bean
  ListClients listClients(CoachingLinks links) {
    return new ListClientsUseCase(links);
  }

  @Bean
  AnswerInvite answerInvite(AccountDirectory accounts, Invites invites) {
    return new AnswerInviteUseCase(accounts, invites);
  }

  @Bean
  ManageLink manageLink(
      CoachingLinks links,
      Invites invites,
      InvitationIssuer issuer,
      PlanLimits planLimits,
      AuditTrail audit,
      Clock clock) {
    return new ManageLinkUseCase(links, invites, issuer, planLimits, audit, clock);
  }
}
