package com.example.githubaccess.config;

import com.example.githubaccess.client.RateLimitGuard;
import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class GithubConfig {

    @Value("${github.api.base-url}")
    private String baseUrl;

    @Value("${github.api.token}")
    private String token;

    @Value("${github.api.connect-timeout-seconds}")
    private int connectTimeoutSeconds;

    @Value("${github.api.read-timeout-seconds}")
    private int readTimeoutSeconds;

    @Bean
    public WebClient githubWebClient(RateLimitGuard rateLimitGuard) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutSeconds * 1000)
                .responseTimeout(Duration.ofSeconds(readTimeoutSeconds));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader(HttpHeaders.USER_AGENT, "github-access-report-service")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter(rateLimitHeaderFilter(rateLimitGuard))
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    private ExchangeFilterFunction rateLimitHeaderFilter(RateLimitGuard rateLimitGuard) {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            String remaining = response.headers().asHttpHeaders().getFirst("X-RateLimit-Remaining");
            String reset = response.headers().asHttpHeaders().getFirst("X-RateLimit-Reset");
            if (remaining != null && reset != null) {
                try {
                    rateLimitGuard.update(Integer.parseInt(remaining), Long.parseLong(reset));
                } catch (NumberFormatException ignored) {
                }
            }
            return Mono.just(response);
        });
    }
}
