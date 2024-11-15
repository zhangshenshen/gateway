package com.example.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"httpbin=http://localhost:${wiremock.server.port}"})
@AutoConfigureWireMock(port = 0)
class GatewayApplicationTests {
    
    @Autowired
    private WebTestClient client;
    
    @Test
    void contextLoads() {
        stubFor(get(urlEqualTo("/get"))
                .willReturn(aResponse()
                        .withBody("{\"headers\":{\"Hello\":\"World\"}}0")
                        .withHeader("Content-Type", "application/json")));
        stubFor(get(urlEqualTo("/delay/3"))
                .willReturn(aResponse()
                        .withBody("no fallback")
                        .withFixedDelay(3000)));
        
        client
                .get()
                .uri("/get")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.headers.Hello")
                .isEqualTo("World");
        
        
        client
                .get()
                .uri("/delay/3")
                .header("Host", "www.circuitbreaker.com")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .consumeWith(response -> assertThat(response.getResponseBody()).isEqualTo("fallback".getBytes()));
        
    }
    
}
