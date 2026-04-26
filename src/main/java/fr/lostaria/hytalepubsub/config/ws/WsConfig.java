package fr.lostaria.hytalepubsub.config.ws;

import fr.lostaria.hytalepubsub.ws.PubSubWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WsConfig implements WebSocketConfigurer {

    private final PubSubWebSocketHandler handler;
    private final WsHandshakeInterceptor interceptor;

    public WsConfig(PubSubWebSocketHandler handler, WsHandshakeInterceptor interceptor) {
        this.handler = handler;
        this.interceptor = interceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/{consumer}")
                .addInterceptors(interceptor)
                .setAllowedOrigins("*");
    }
}
