package com.twinsolution.construction.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "openai")
@Getter
@Setter
public class OpenAiProperties {
    private String apiKey;
    private String model;
    private Integer maxTokens;
    private Double temperature;
    private String apiUrl;
}
