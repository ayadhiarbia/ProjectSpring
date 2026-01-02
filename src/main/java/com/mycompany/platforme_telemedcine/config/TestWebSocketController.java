package com.mycompany.platforme_telemedcine.config;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestWebSocketController {

    @GetMapping("/test/websocket")
    public String testWebSocketPage() {
        return "test/websocket-test";
    }

    @MessageMapping("/test")
    @SendTo("/topic/test")
    public String testWebSocket(String message) {
        return "Received: " + message + " at " + new java.util.Date();
    }
}