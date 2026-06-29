package juribook.lawyer_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import juribook.lawyer_service.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtre JWT du lawyer-service.
 *
 * Extrait et valide le JWT à chaque requête.
 * Si le token est valide, construit un Authentication Spring Security
 * avec le userId et le rôle extraits des claims.
 *
 * barNumber peut être absent du token (claim optionnel), on le lit
 * défensivement avec getOrDefault pour éviter un NPE.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Pas de header Authorization → passer au filtre suivant (routes publiques)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Long userId  = jwtService.extractUserId(token);
            String role  = jwtService.extractRole(token);

            // barNumber est optionnel, peut ne pas être dans les claims du token
            // On ne lève pas d'exception s'il est absent
            String barNumber = jwtService.extractBarNumber(token);

            if (userId == null || role == null) {
                chain.doFilter(request, response);
                return;
            }

            List<SimpleGrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority("ROLE_" + role));

            // principal = userId, credentials = barNumber (peut être null)
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, barNumber, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (Exception e) {
            // Token invalide ou claim manquant → ne pas authentifier
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}