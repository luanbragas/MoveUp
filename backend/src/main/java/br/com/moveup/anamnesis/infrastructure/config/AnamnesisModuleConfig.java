package br.com.moveup.anamnesis.infrastructure.config;

import br.com.moveup.anamnesis.application.port.in.ManageAnamnesis;
import br.com.moveup.anamnesis.application.port.in.ManageRestrictions;
import br.com.moveup.anamnesis.application.port.out.AnamnesisStore;
import br.com.moveup.anamnesis.application.port.out.Restrictions;
import br.com.moveup.anamnesis.application.usecase.ManageAnamnesisUseCase;
import br.com.moveup.anamnesis.application.usecase.ManageRestrictionsUseCase;
import br.com.moveup.audit.api.AuditTrail;
import br.com.moveup.coaching.api.CoachingRoster;
import br.com.moveup.coaching.api.LinkDirectory;
import br.com.moveup.shared.domain.IdGenerator;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Casos de uso do módulo anamnesis (adaptadores jOOQ são beans do pacote persistence). */
@Configuration(proxyBeanMethods = false)
class AnamnesisModuleConfig {

  @Bean
  ManageAnamnesis manageAnamnesis(
      AnamnesisStore store,
      CoachingRoster roster,
      LinkDirectory links,
      AuditTrail audit,
      IdGenerator ids,
      Clock clock) {
    return new ManageAnamnesisUseCase(store, roster, links, audit, ids, clock);
  }

  @Bean
  ManageRestrictions manageRestrictions(
      Restrictions restrictions, LinkDirectory links, IdGenerator ids) {
    return new ManageRestrictionsUseCase(restrictions, links, ids);
  }
}
