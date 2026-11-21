package be.ucll.security;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Lets a WebSocket connect only with a credential, and records who it is.
 *
 * The hub connects with {@code ?client=esp} and {@code Authorization: Bearer <HUB_TOKEN>}.
 * Browsers cannot set headers on a WebSocket, so they pass their short-lived token as {@code ?token=}.
 */
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    public static final String CLIENT_ATTRIBUTE = "battlecars.client";

    public enum Client { HUB, BROWSER }

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);
    private static final String BEARER = "Bearer ";

    private final ApiTokens tokens;

    public WebSocketAuthInterceptor(ApiTokens tokens) {
        this.tokens = tokens;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        MultiValueMap<String, String> query = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();

        if ("esp".equals(query.getFirst("client"))) {
            String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith(BEARER) && tokens.isValidHubToken(header.substring(BEARER.length()))) {
                attributes.put(CLIENT_ATTRIBUTE, Client.HUB);
                return true;
            }
            log.warn("Rejected a hub connection from {} with a missing or wrong HUB_TOKEN", request.getRemoteAddress());
        } else if (tokens.isValidBrowserToken(query.getFirst("token"))) {
            attributes.put(CLIENT_ATTRIBUTE, Client.BROWSER);
            return true;
        } else {
            log.warn("Rejected a browser connection from {} with a missing or expired token", request.getRemoteAddress());
        }

        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Nothing to do once connected.
    }
}
