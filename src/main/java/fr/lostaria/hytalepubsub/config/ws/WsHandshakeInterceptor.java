package fr.lostaria.hytalepubsub.config.ws;

import com.auth0.jwt.interfaces.DecodedJWT;
import fr.lostaria.hytalepubsub.config.JwtVerifierService;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

@Component
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtVerifierService jwtVerifierService;

    public WsHandshakeInterceptor(JwtVerifierService jwtVerifierService) {
        this.jwtVerifierService = jwtVerifierService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        Map<String, String> uriVars = (Map<String, String>) attributes.get(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (uriVars != null) {
            attributes.put("consumer", uriVars.get("consumer"));
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        String token = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7).trim()
                : null;

        if (token == null || token.isBlank()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            DecodedJWT jwt = jwtVerifierService.verify(token);
            attributes.put("deviceId", jwt.getSubject());
            return true;
        } catch (Exception e) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }
}
