package juribook.lawyer_service.config;

import juribook.lawyer_service.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration Spring Security du lawyer-service.
 *
 * Routes GET publiques (pas de JWT requis) :
 *   GET /api/lawyers           → recherche paginée
 *   GET /api/lawyers/{id}      → profil public
 *   GET /api/specialties       → liste des spécialités
 *   GET /actuator/health       → health check Docker
 *   GET /swagger-ui/**         → documentation API
 *   GET /v3/api-docs/**        → spec OpenAPI
 *
 * Routes protégées LAWYER uniquement :
 *   POST /api/lawyers/profile  → création du profil
 *   GET  /api/lawyers/profile  → consultation du profil
 *   PUT  /api/lawyers/profile  → modification du profil
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                    // ── Routes publiques GET ────────────────────
                    .requestMatchers(HttpMethod.GET,  "/api/lawyers").permitAll()
                    .requestMatchers(HttpMethod.GET,  "/api/lawyers/*").permitAll()
                    .requestMatchers(HttpMethod.GET,  "/api/specialties").permitAll()
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                    // ── Routes LAWYER uniquement ────────────────
                    // ⚠️ /api/lawyers/profile doit être AVANT /api/lawyers/*
                    // pour ne pas être capturé par la règle publique
                    .requestMatchers(HttpMethod.POST, "/api/lawyers/profile").hasRole("LAWYER")
                    .requestMatchers(HttpMethod.GET,  "/api/lawyers/profile").hasRole("LAWYER")
                    .requestMatchers(HttpMethod.PUT,  "/api/lawyers/profile").hasRole("LAWYER")
                    // ── Tout le reste → authentification requise
                    .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}