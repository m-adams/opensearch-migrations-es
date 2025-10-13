package org.opensearch.migrations.bulkload.common.http;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class ApiKeyAuthTransformer implements RequestTransformer {
    private final String apiKey;

    @Override
    public Mono<TransformedRequest> transform(String method, String path, Map<String, List<String>> headers, Mono<ByteBuffer> body) {
        var newHeaders = new HashMap<>(headers);
        newHeaders.put("Authorization", List.of("ApiKey " + apiKey));
        return Mono.just(new TransformedRequest(newHeaders, body));
    }
}
