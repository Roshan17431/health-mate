package com.example.health.mate;

import com.example.health.mate.support.ApiIntegrationSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.net.URI;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:cors;DB_CLOSE_DELAY=-1",
    "app.cors.allowed-origins=https://care.example.test,http://localhost:3300"})
class CorsIntegrationTests extends ApiIntegrationSupport {
    private HttpResponse<String> preflight(String origin) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/login"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).header("Origin", origin)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type").build(), HttpResponse.BodyHandlers.ofString());
    }
    @Test void trustedOriginIsAllowedExactly() throws Exception {
        var response = preflight("https://care.example.test");
        assertEquals(200, response.statusCode());
        assertEquals("https://care.example.test", response.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
    }
    @Test void untrustedAndSuffixOriginsAreRejected() throws Exception {
        for (String origin : new String[]{"https://evil.example", "https://care.example.test.evil.example", "null"}) {
            var response = preflight(origin);
            assertEquals(403, response.statusCode(), origin);
            assertTrue(response.headers().firstValue("Access-Control-Allow-Origin").isEmpty());
        }
    }
}
