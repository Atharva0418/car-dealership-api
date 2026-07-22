package com.atharva.dealership.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @RequestMapping(method = RequestMethod.HEAD, path = "/api/health")
    public ResponseEntity<Void> health() {
        System.out.println("Server is running");
        return ResponseEntity.ok().build();
    }
}
