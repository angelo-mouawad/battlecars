package be.ucll.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authenticates REST calls that carry a valid browser token in {@code Authorization: Bearer ...}.
 * Requests without one stay anonymous and are turned away by the security rules.
 *
 * Not a Spring bean on purpose, so Boot does not also register it as a plain servlet filter.
 */
public class BearerTokenFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final ApiTokens tokens;

    public BearerTokenFilter(ApiTokens tokens) {
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIX) && tokens.isValidBrowserToken(header.substring(PREFIX.length()))) {
            var authentication = new UsernamePasswordAuthenticationToken(
                    "frontend", null, List.of(new SimpleGrantedAuthority("ROLE_FRONTEND")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(request, response);
    }
}
