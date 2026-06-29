package juribook.lawyer_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // URLs publiques - pas de token JWT requis
    private static final String[] PUBLIC_URLS = {
        // Lawyer
        "/api/lawyer/**",
        // Swagger UI
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs",
        "/v3/api-docs/**",
        // Actuator health (pour les healthchecks Docker)
        "/actuator/health"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Désactiver CSRF (API REST stateless)
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(lawyer -> lawyer
                .requestMatchers(PUBLIC_URLS).permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}