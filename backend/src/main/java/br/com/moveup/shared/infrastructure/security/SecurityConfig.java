package br.com.moveup.shared.infrastructure.security;

import br.com.moveup.shared.infrastructure.web.Problems;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

/**
 * API stateless protegida por JWT do Firebase (ARQUITETURA, seção 8): sem sessão, sem CSRF (não há
 * cookie), sem CORS (não há cliente web; o site estático não chama a API), headers de segurança
 * restritivos. Tudo em {@code /v1} exige token; o que não está listado é negado.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class SecurityConfig {

  @Bean
  SecurityFilterChain apiSecurity(
      HttpSecurity http, AppUserResolver resolver, Problems problems, ObjectMapper objectMapper)
      throws Exception {
    var handlers = new ProblemSecurityHandlers(problems, objectMapper);
    return http.csrf(AbstractHttpConfigurer::disable)
        .cors(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .requestCache(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(
            h ->
                h.contentSecurityPolicy(
                        c -> c.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                    .referrerPolicy(r -> r.policy(ReferrerPolicy.NO_REFERRER)))
        .authorizeHttpRequests(
            a ->
                // OpenAPI/Swagger só existem quando springdoc está ligado (local); em produção, 404
                a.requestMatchers(
                        "/actuator/health",
                        "/actuator/health/**",
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml",
                        "/swagger-ui.html",
                        "/swagger-ui/**")
                    .permitAll()
                    .requestMatchers("/v1/**")
                    .authenticated()
                    .anyRequest()
                    .denyAll())
        .oauth2ResourceServer(
            o ->
                o.jwt(Customizer.withDefaults())
                    .authenticationEntryPoint(handlers)
                    .accessDeniedHandler(handlers))
        .exceptionHandling(e -> e.authenticationEntryPoint(handlers).accessDeniedHandler(handlers))
        .addFilterAfter(new CurrentAppUserFilter(resolver), BearerTokenAuthenticationFilter.class)
        .build();
  }

  @Bean
  JwtDecoder jwtDecoder(
      @Value("${moveup.auth.firebase.project-id}") String projectId, Clock clock) {
    return FirebaseJwt.decoder(projectId, clock);
  }
}
