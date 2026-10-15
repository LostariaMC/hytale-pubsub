package fr.lostaria.hytalepubsub.payload;

import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.JsonNode;

@Getter
@Setter
public class MessageEnvelope {

    private String consumer;

    private String type;

    private String producer;

    private JsonNode payload;

}
