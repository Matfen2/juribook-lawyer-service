package juribook.lawyer_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Configuration CORS pour l'auth-service.
 *
 * Autorise les requêtes depuis le frontend React (localhost:5173)
 * vers les endpoints de l'auth-service (localhost:8081).
 *
 * Sans ce filtre, le navigateur bloque toutes les requêtes
 * cross-origin (frontend → backend) avec l'erreur :
 * "No 'Access-Control-Allow-Origin' header is present"
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Origines autorisées — frontend React en développement
        config.addAllowedOrigin("http://localhost:5173");
        // En production, remplacer par le domaine réel
        // config.addAllowedOrigin("https://juribook.fr");

        // Méthodes HTTP autorisées
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");  // requête preflight du navigateur

        // Headers autorisés dans les requêtes du frontend
        config.addAllowedHeader("Authorization");   // pour le JWT Bearer
        config.addAllowedHeader("Content-Type");
        config.addAllowedHeader("Accept");

        // Autoriser le navigateur à lire le header Authorization dans la réponse
        config.addExposedHeader("Authorization");

        // Autoriser l'envoi de cookies/credentials si nécessaire
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Appliquer la config CORS à tous les endpoints
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}