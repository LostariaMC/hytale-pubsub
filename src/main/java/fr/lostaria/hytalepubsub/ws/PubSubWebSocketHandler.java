package fr.lostaria.hytalepubsub.ws;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import fr.lostaria.hytalepubsub.payload.MessageEnvelope;
import fr.lostaria.hytalepubsub.services.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(PubSubWebSocketHandler.class);

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
        LOGGER.info("CONNECTED consumer={} deviceId={} session={}", consumer, session.getAttributes().get("deviceId"), session.getId());

        Thread thread = Thread.ofVirtual().start(() -> {
            while (session.isOpen() && !Thread.currentThread().isInterrupted()) {
                MessageEnvelope msg = messageService.waitNext(consumer, Duration.ofSeconds(10));
                if (msg == null) continue;

                if (!session.isOpen()) {
                    LOGGER.warn("LOST {} -> {} type={} (session closed)", msg.getProducer(), consumer, msg.getType());
                    break;
                }

                try {
                    String json = objectMapper.writeValueAsString(msg);
                    synchronized (session) {
                        session.sendMessage(new TextMessage(json));
                    }
                    LOGGER.info("DELIVERED {} -> {} type={}", msg.getProducer(), consumer, msg.getType());
                } catch (IOException e) {
                    LOGGER.warn("LOST {} -> {} type={} ({})", msg.getProducer(), consumer, msg.getType(), e.toString());
                    break;
                }
            }
        });

        listenerThreads.put(session.getId(), thread);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Object from = session.getAttributes().get("consumer");

        MessageEnvelope envelope;
        try {
            envelope = objectMapper.readValue(message.getPayload(), MessageEnvelope.class);
        } catch (JacksonException e) {
            LOGGER.warn("REJECTED from={} invalid JSON: {}", from, e.getOriginalMessage());
            return;
        }

        String missing = missingField(envelope);
        if (missing != null) {
            LOGGER.warn("REJECTED from={} type={} missing field: {}", from, envelope.getType(), missing);
            return;
        }

        messageService.send(envelope.getConsumer(), envelope);
    }

    private static String missingField(MessageEnvelope envelope) {
        if (envelope == null) return "envelope";
        if (isBlank(envelope.getConsumer())) return "consumer";
        if (isBlank(envelope.getProducer())) return "producer";
        if (isBlank(envelope.getType())) return "type";
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        LOGGER.info("DISCONNECTED consumer={} session={} status={}", session.getAttributes().get("consumer"), session.getId(), status);
        Thread thread = listenerThreads.remove(session.getId());
        if (thread != null) {
            thread.interrupt();
        }
    }
}
