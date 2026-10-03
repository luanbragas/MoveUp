package br.com.moveup.shared.infrastructure.security;

import br.com.moveup.shared.infrastructure.web.Problems;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

/**
 * API stateless protegida por JWT do Firebase (ARQUITETURA, seção 8): sem sessão, sem CSRF (não há
 * cookie), headers de segurança restritivos. Tudo em {@code /v1} exige token; o que não está
 * listado é negado. Exceção única: a autorização do responsável pelo menor, chamada pela página do
 * site estático sem login (o segredo do link é a credencial), com CORS só para essa rota e só para
 * a origem do site.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class SecurityConfig {

  private static final String GUARDIAN_AUTHORIZATIONS = "/v1/guardian-authorizations/**";

  @Bean
  SecurityFilterChain apiSecurity(
      HttpSecurity http,
      AppUserResolver resolver,
      Problems problems,
      JsonMapper jsonMapper,
      @Value("${moveup.guardian.allowed-origins}") List<String> guardianPageOrigins)
      throws Exception {
    var handlers = new ProblemSecurityHandlers(problems, jsonMapper);
    return http.csrf(AbstractHttpConfigurer::disable)
        .cors(c -> c.configurationSource(guardianPageCors(guardianPageOrigins)))
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
                    .requestMatchers(HttpMethod.POST, GUARDIAN_AUTHORIZATIONS)
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

  private static UrlBasedCorsConfigurationSource guardianPageCors(List<String> origins) {
    var config = new CorsConfiguration();
    config.setAllowedOrigins(origins);
    config.setAllowedMethods(List.of("POST"));
    config.setAllowedHeaders(List.of("Content-Type"));
    config.setAllowCredentials(false);
    config.setMaxAge(Duration.ofHours(1));
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration(GUARDIAN_AUTHORIZATIONS, config);
    return source;
  }

  @Bean
  JwtDecoder jwtDecoder(
      @Value("${moveup.auth.firebase.project-id}") String projectId, Clock clock) {
    return FirebaseJwt.decoder(projectId, clock);
  }
}
