package besu.optimization.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * REST Client Configuration
 *
 * Paper reference (Section 3.2):
 * "The WebClient is configured with connection pooling (maxConnections=500)
 * and timeout settings (responseTimeout=30s) to handle high concurrency."
 *
 * Key settings:
 * - 500 total connections (handles high concurrency)
 * - 500 connections per route (to middleware)
 * - 3s connect timeout
 * - 30s read timeout (accounts for 4-10s blockchain finality)
 * - Idle connection eviction (30s)
 */
@Configuration
public class RestClientConfig {

    @Value("${middleware.base-url:http://localhost:3000}")
    private String middlewareBaseUrl;

    @Bean
    public CloseableHttpClient httpClient() {
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(500)       // Total max connections
                .setMaxConnPerRoute(500)    // Max connections per route (middleware)
                .build();

        // Configure timeouts via RequestConfig
        var requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(3))
                .setResponseTimeout(Timeout.ofSeconds(30))  // Long timeout for blockchain finality
                .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .build();
    }

    @Bean
    public HttpComponentsClientHttpRequestFactory requestFactory(CloseableHttpClient httpClient) {
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }

    /**
     * RestClient for middleware communication
     * Non-blocking when used with Virtual Threads
     */
    @Bean
    public RestClient middlewareRestClient(HttpComponentsClientHttpRequestFactory requestFactory) {
        return RestClient.builder()
                .baseUrl(middlewareBaseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
