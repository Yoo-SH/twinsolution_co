package com.twinsolution.construction.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ollama")
@Getter
@Setter
public class OllamaProperties {
    private String baseUrl = "http://localhost:11434";
    private String defaultModel = "llama2";
    private Integer timeout = 300;  // 초 단위
}
