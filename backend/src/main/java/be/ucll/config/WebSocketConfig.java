package be.ucll.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import be.ucll.security.WebSocketAuthInterceptor;
import be.ucll.websocket.BattleCarsWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final BattleCarsWebSocketHandler handler;
    private final WebSocketAuthInterceptor authInterceptor;
    private final String[] allowedOrigins;

    public WebSocketConfig(BattleCarsWebSocketHandler handler,
                           WebSocketAuthInterceptor authInterceptor,
                           @Value("${cors.allowed-origins}") String allowedOrigins) {
        this.handler = handler;
        this.authInterceptor = authInterceptor;
        this.allowedOrigins = CorsConfig.parseOrigins(allowedOrigins);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws")
                .addInterceptors(authInterceptor)
                .setAllowedOriginPatterns(allowedOrigins);
    }
}
