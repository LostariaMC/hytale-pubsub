package fr.lostaria.hytalepubsub.payload;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageEnvelope {

    @NotBlank
    private String type;

    private JsonNode payload;

}
