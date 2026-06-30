package juribook.lawyer_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Service de validation des JWT dans le lawyer-service.
 *
 * Le lawyer-service ne génère pas de JWT, il valide uniquement
 * les tokens émis par l'auth-service en utilisant le même secret.
 *
 * Claims extraits :
 *   - sub       → email de l'utilisateur
 *   - id        → authUserId (Long)
 *   - role      → CLIENT | LAWYER | ADMIN
 *   - barNumber → numéro de barreau (optionnel — peut être absent)
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return extractClaims(token).get("id", Long.class);
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    /**
     * barNumber est optionnel dans le JWT.
     * Retourne null si le claim n'existe pas (pas d'exception).
     */
    public String extractBarNumber(String token) {
        try {
            return extractClaims(token).get("barNumber", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}