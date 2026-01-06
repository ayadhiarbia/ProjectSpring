package com.mycompany.platforme_telemedcine.dto;

public class SearchResultDTO {
    private String type;
    private Long id;
    private String title;
    private String description;
    private String date;
    private String url;

    public SearchResultDTO(String type, Long id, String title, String description, String date, String url) {
        this.type = type;
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.url = url;
    }

    // Getters
    public String getType() { return type; }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getDate() { return date; }
    public String getUrl() { return url; }
}