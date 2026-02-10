package fr.lostaria.hytalepubsub.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/public")
public class PublicController {

    @GetMapping("/ping")
    public ResponseEntity ping() {
        return ResponseEntity.status(HttpStatus.OK).body(Map.of(HttpStatus.OK, "PONG"));
    }

}
