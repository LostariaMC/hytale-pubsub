package fr.lostaria.hytalepubsub.payload;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.JsonNode;

@Getter
@Setter
public class MessageEnvelope {

    @NotBlank
    private String type;

    private JsonNode payload;

}
