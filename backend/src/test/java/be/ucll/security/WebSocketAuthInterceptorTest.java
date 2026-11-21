package be.ucll.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import be.ucll.security.WebSocketAuthInterceptor.Client;

class WebSocketAuthInterceptorTest {

    private static final String HUB = "test-hub-token-0123456789";

    private final ApiTokens tokens = new ApiTokens("test-api-secret-0123456789", HUB);
    private final WebSocketAuthInterceptor interceptor = new WebSocketAuthInterceptor(tokens);
    private final Map<String, Object> attributes = new HashMap<>();
    private final MockHttpServletResponse servletResponse = new MockHttpServletResponse();

    private boolean handshake(String query, String authorization) {
        URI uri = URI.create("ws://localhost/ws" + (query == null ? "" : "?" + query));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri.getPath());
        request.setQueryString(uri.getQuery());
        if (authorization != null) request.addHeader("Authorization", authorization);
        ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse);
        return interceptor.beforeHandshake(new ServletServerHttpRequest(request), response, null, attributes);
    }

    @Test
    void acceptsTheHubWithItsToken() {
        assertThat(handshake("client=esp", "Bearer " + HUB)).isTrue();
        assertThat(attributes).containsEntry(WebSocketAuthInterceptor.CLIENT_ATTRIBUTE, Client.HUB);
    }

    @Test
    void rejectsAHubWithoutTheToken() {
        assertThat(handshake("client=esp", null)).isFalse();
        assertThat(handshake("client=esp", "Bearer wrong-token-0123456789")).isFalse();
        assertThat(servletResponse.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(attributes).isEmpty();
    }

    @Test
    void doesNotTreatABrowserTokenAsTheHub() {
        String browserToken = tokens.issueBrowserToken(Duration.ofMinutes(15));
        assertThat(handshake("client=esp&token=" + browserToken, null)).isFalse();
    }

    @Test
    void acceptsABrowserWithAFreshToken() {
        assertThat(handshake("token=" + tokens.issueBrowserToken(Duration.ofMinutes(15)), null)).isTrue();
        assertThat(attributes).containsEntry(WebSocketAuthInterceptor.CLIENT_ATTRIBUTE, Client.BROWSER);
    }

    @Test
    void rejectsABrowserWithoutAToken() {
        assertThat(handshake(null, null)).isFalse();
        assertThat(handshake("token=123.abc", null)).isFalse();
        assertThat(servletResponse.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }
}
