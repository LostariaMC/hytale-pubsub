package fr.lostaria.hytalepubsub.services;

import fr.lostaria.hytalepubsub.payload.MessageEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Service
public class MessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageService.class);

    private final Map<String, BlockingQueue<MessageEnvelope>> queues = new ConcurrentHashMap<>();

    private BlockingQueue<MessageEnvelope> queue(String consumer) {
        return queues.computeIfAbsent(consumer, id -> new LinkedBlockingQueue<>());
    }

    public void send(String consumer, MessageEnvelope message) {
        BlockingQueue<MessageEnvelope> queue = queue(consumer);
        queue.offer(message);
        LOGGER.info("QUEUED {} -> {} type={} pending={}", message.getProducer(), consumer, message.getType(), queue.size());
    }

    public MessageEnvelope waitNext(String consumer, Duration timeout) {
        try {
            return queue(consumer).poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

}
