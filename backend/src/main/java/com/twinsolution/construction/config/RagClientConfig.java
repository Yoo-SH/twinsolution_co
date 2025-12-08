package com.twinsolution.construction.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class RagClientConfig {

    @Value("${rag.server.url:http://localhost:8000}")
    private String ragServerUrl;

    @Bean
    public WebClient ragWebClient() {
        // RAG 임베딩은 시간이 오래 걸릴 수 있으므로 타임아웃을 2분으로 설정
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000) // 연결 타임아웃: 10초
                .responseTimeout(Duration.ofMinutes(2)) // 응답 타임아웃: 2분
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(120, TimeUnit.SECONDS)) // 읽기 타임아웃: 2분
                                .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS))); // 쓰기 타임아웃: 30초

        return WebClient.builder()
                .baseUrl(ragServerUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
