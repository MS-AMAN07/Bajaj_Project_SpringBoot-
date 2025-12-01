package com.healthrx.contest_solver.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookResponse {
    // Validates if the API sends "webhook" OR "webhookUrl"
    @JsonAlias({"webhook", "url", "webhookUrl"}) 
    private String webhook;
    
    private String accessToken;
}