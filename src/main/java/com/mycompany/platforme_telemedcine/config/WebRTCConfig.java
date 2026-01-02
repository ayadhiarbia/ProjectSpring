package com.mycompany.platforme_telemedcine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSocket
public class WebRTCConfig {

    @Bean
    public List<String> stunServers() {
        return Arrays.asList(
                "stun:stun.l.google.com:19302",
                "stun:stun1.l.google.com:19302",
                "stun:stun2.l.google.com:19302",
                "stun:stun3.l.google.com:19302",
                "stun:stun4.l.google.com:19302",
                "stun:stun.voipbuster.com:3478",
                "stun:stun.voipstunt.com:3478"
        );
    }

    @Bean
    public List<TurnServerConfig> turnServers() {
        // For production, use real TURN servers with credentials
        return Arrays.asList(
                new TurnServerConfig("turn:turn.example.com:3478", "username", "password"),
                new TurnServerConfig("turn:turn.example.com:5349", "username", "password", "turns")
        );
    }

    public static class TurnServerConfig {
        private String url;
        private String username;
        private String credential;
        private String protocol;

        public TurnServerConfig(String url, String username, String credential) {
            this(url, username, credential, "turn");
        }

        public TurnServerConfig(String url, String username, String credential, String protocol) {
            this.url = url;
            this.username = username;
            this.credential = credential;
            this.protocol = protocol;
        }

        // Getters
        public String getUrl() { return url; }
        public String getUsername() { return username; }
        public String getCredential() { return credential; }
        public String getProtocol() { return protocol; }
    }
}