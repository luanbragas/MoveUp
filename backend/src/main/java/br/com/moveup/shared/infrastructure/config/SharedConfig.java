package br.com.moveup.shared.infrastructure.config;

import br.com.moveup.shared.domain.IdGenerator;
import br.com.moveup.shared.infrastructure.id.UuidV7Generator;
import br.com.moveup.shared.infrastructure.persistence.RlsTransactionManager;
import br.com.moveup.shared.infrastructure.security.CurrentAppUser;
import java.time.Clock;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/** Beans do núcleo compartilhado. */
@Configuration(proxyBeanMethods = false)
class SharedConfig {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  IdGenerator idGenerator(Clock clock) {
    return new UuidV7Generator(clock);
  }

  /**
   * Substitui o gerenciador de transações padrão do Spring Boot (que recua quando existe um), então
   * toda transação, inclusive as do jOOQ, passa pelo {@code set_config('app.user_id', ...)}.
   */
  @Bean
  PlatformTransactionManager transactionManager(
      DataSource dataSource, ObjectProvider<CurrentAppUser> currentAppUser) {
    CurrentAppUser noUser = Optional::empty;
    return new RlsTransactionManager(dataSource, currentAppUser.getIfAvailable(() -> noUser));
  }
}
