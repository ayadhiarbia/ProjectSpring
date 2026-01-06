package com.mycompany.platforme_telemedcine.Controllers.helpers;

public class TurnServer {
    private String url;
    private String username;
    private String credential;

    public TurnServer(String url, String username, String credential) {
        this.url = url;
        this.username = username;
        this.credential = credential;
    }

    // Getters
    public String getUrl() { return url; }
    public String getUsername() { return username; }
    public String getCredential() { return credential; }
}