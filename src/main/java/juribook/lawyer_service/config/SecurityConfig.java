package juribook.lawyer_service.config;

import juribook.lawyer_service.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration Spring Security + CORS du lawyer-service.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                    // Routes publiques
                    .requestMatchers(HttpMethod.GET, "/api/lawyers").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/specialties").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                    // Routes LAWYER - déclarées avant /api/lawyers/**
                    .requestMatchers(HttpMethod.POST, "/api/lawyers/profile").hasRole("LAWYER")
                    .requestMatchers(HttpMethod.GET,  "/api/lawyers/profile").hasRole("LAWYER")
                    .requestMatchers(HttpMethod.PUT,  "/api/lawyers/profile").hasRole("LAWYER")
                    // Route CLIENT - laisser un avis
                    .requestMatchers(HttpMethod.POST, "/api/reviews").hasRole("CLIENT")
                    // Routes ADMIN - modération des avis,
                    // déclarées avant tout catch-all pour être sûres de matcher
                    .requestMatchers(HttpMethod.GET, "/api/reviews/moderation").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/reviews/*/hide").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PATCH, "/api/reviews/*/unhide").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/reviews/*").hasRole("ADMIN")
                    // Profils publics
                    .requestMatchers(HttpMethod.GET, "/api/lawyers/**").permitAll()
                    .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "https://juribook.fr", "https://www.juribook.fr"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}