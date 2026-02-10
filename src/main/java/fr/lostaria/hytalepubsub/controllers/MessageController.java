package fr.lostaria.hytalepubsub.controllers;

import fr.lostaria.hytalepubsub.payload.MessageEnvelope;
import fr.lostaria.hytalepubsub.services.MessageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/{consumer}")
    public ResponseEntity<MessageEnvelope> messageListener(
            @PathVariable String consumer,
            @RequestParam(defaultValue = "25") int timeoutSeconds
    ) {
        int t = Math.max(1, Math.min(timeoutSeconds, 30));
        MessageEnvelope msg = messageService.waitNext(consumer, Duration.ofSeconds(t));

        if (msg == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(msg);
    }

    @PostMapping("/{consumer}")
    public ResponseEntity sendMessage(
            @PathVariable String consumer,
            @Valid @RequestBody MessageEnvelope payload
    ) {
        messageService.send(consumer, payload);
        return ResponseEntity.accepted().build();
    }

}
