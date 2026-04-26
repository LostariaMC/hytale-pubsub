package fr.lostaria.hytalepubsub.ws;

import tools.jackson.databind.ObjectMapper;
import fr.lostaria.hytalepubsub.payload.MessageEnvelope;
import fr.lostaria.hytalepubsub.services.MessageService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PubSubWebSocketHandler extends TextWebSocketHandler {

    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    private final Map<String, Thread> listenerThreads = new ConcurrentHashMap<>();

    public PubSubWebSocketHandler(MessageService messageService, ObjectMapper objectMapper) {
        this.messageService = messageService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String consumer = (String) session.getAttributes().get("consumer");

        Thread thread = Thread.ofVirtual().start(() -> {
            while (session.isOpen() && !Thread.currentThread().isInterrupted()) {
                MessageEnvelope msg = messageService.waitNext(consumer, Duration.ofSeconds(10));
                if (msg != null && session.isOpen()) {
                    try {
                        String json = objectMapper.writeValueAsString(msg);
                        synchronized (session) {
                            session.sendMessage(new TextMessage(json));
                        }
                    } catch (IOException e) {
                        break;
                    }
                }
            }
        });

        listenerThreads.put(session.getId(), thread);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        MessageEnvelope envelope = objectMapper.readValue(message.getPayload(), MessageEnvelope.class);
        messageService.send(envelope.getConsumer(), envelope);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Thread thread = listenerThreads.remove(session.getId());
        if (thread != null) {
            thread.interrupt();
        }
    }
}
