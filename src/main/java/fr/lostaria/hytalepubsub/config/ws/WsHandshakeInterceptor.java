package fr.lostaria.hytalepubsub.config.ws;

import com.auth0.jwt.interfaces.DecodedJWT;
import fr.lostaria.hytalepubsub.config.JwtVerifierService;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

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
        String path = request.getURI().getPath();
        String consumer = path.substring(path.lastIndexOf('/') + 1);
        if (consumer.isBlank()) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }
        attributes.put("consumer", consumer);

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
