package org.opensearch.migrations.bulkload.workcoordination;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.opensearch.migrations.Version;
import org.opensearch.migrations.bulkload.common.http.ConnectionContext;
import org.opensearch.migrations.bulkload.version_es_8_x.OpenSearchWorkCoordinator_ES_8_Serverless;
import org.opensearch.migrations.bulkload.version_os_2_11.OpenSearchWorkCoordinator_OS_2_11;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Assertions;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for WorkCoordinatorFactory serverless routing logic.
 * 
 * Tests the factory routing behavior:
 * 1. Routes to serverless coordinator when target is ELASTICSEARCH_SERVERLESS
 * 2. Routes to standard coordinator for OpenSearch/Elasticsearch targets
 * 3. Target type detection from ConnectionContext
 * 4. Error handling for unknown target types
 */
@Slf4j
class WorkCoordinatorFactory_ServerlessTest {

    @AllArgsConstructor
    public static class MockHttpClient implements AbstractedHttpClient {
        AbstractHttpResponse response;

        @Override
        public AbstractHttpResponse makeRequest(String method, String path, Map<String, String> headers, String payload) {
            return response;
        }
    }

    @AllArgsConstructor
    @Getter
    public static class TestResponse implements AbstractedHttpClient.AbstractHttpResponse {
        int statusCode;
        String statusText;
        byte[] payloadBytes;

        @Override
        public Stream<Map.Entry<String, String>> getHeaders() {
            return Stream.empty();
        }
    }

    @Test
    void testRoutesToServerlessCoordinatorWhenTargetIsServerless() throws Exception {
        // Setup
        var factory = new WorkCoordinatorFactory(Version.fromString("OS 2.11"));
        var client = mockServerlessHttpClient();

        // Action
        try (var coordinator = factory.get(client, 3600, "test-worker")) {
            // Assertions
            log.info("Coordinator class: {}", coordinator.getClass().getName());
            assertTrue(coordinator instanceof OpenSearchWorkCoordinator_ES_8_Serverless,
                "Factory should return serverless coordinator for ELASTICSEARCH_SERVERLESS target");
        }
    }

    @Test
    void testRoutesToServerlessCoordinatorWithClockAndConsumer() throws Exception {
        // Setup
        var factory = new WorkCoordinatorFactory(Version.fromString("OS 2.11"));
        var client = mockServerlessHttpClient();
        var workItemRef = new AtomicReference<IWorkCoordinator.WorkItemAndDuration>();

        // Action
        try (var coordinator = factory.get(
            client,
            3600,
            "test-worker",
            Clock.systemUTC(),
            workItemRef::set
        )) {
            // Assertions
            log.info("Coordinator class (with clock): {}", coordinator.getClass().getName());
            assertTrue(coordinator instanceof OpenSearchWorkCoordinator_ES_8_Serverless,
                "Factory should return serverless coordinator with clock and consumer");
        }
    }

    @Test
    void testRoutesToStandardCoordinatorForOpenSearch() throws Exception {
        // Setup
        var factory = new WorkCoordinatorFactory(Version.fromString("OS 2.11"));
        var client = mockOpenSearchHttpClient();

        // Action
        try (var coordinator = factory.get(client, 3600, "test-worker")) {
            // Assertions
            log.info("Coordinator class: {}", coordinator.getClass().getName());
            assertTrue(coordinator instanceof OpenSearchWorkCoordinator_OS_2_11,
                "Factory should return OS_2_11 coordinator for OpenSearch target");
        }
    }

    @Test
    void testRoutesToStandardCoordinatorForElasticsearch() throws Exception {
        // Setup
        var factory = new WorkCoordinatorFactory(Version.fromString("ES 7.10"));
        var client = mockElasticsearchHttpClient();

        // Action
        try (var coordinator = factory.get(client, 3600, "test-worker")) {
            // Assertions
            log.info("Coordinator class: {}", coordinator.getClass().getName());
            assertTrue(coordinator instanceof OpenSearchWorkCoordinator_OS_2_11,
                "Factory should return OS_2_11 coordinator for standard Elasticsearch target");
        }
    }

    @Test
    void testServerlessDetectionWithNonCoordinateWorkHttpClient() throws Exception {
        // Setup
        var factory = new WorkCoordinatorFactory(Version.fromString("OS 2.11"));
        
        // Use a different AbstractedHttpClient implementation (not CoordinateWorkHttpClient)
        var mockResponse = new TestResponse(200, "ok", new byte[]{});
        var client = new MockHttpClient(mockResponse);

        // Action
        try (var coordinator = factory.get(client, 3600, "test-worker")) {
            // Assertions
            log.info("Coordinator class: {}", coordinator.getClass().getName());
            // When httpClient is not CoordinateWorkHttpClient, it should default to version-based routing
            assertTrue(coordinator instanceof OpenSearchWorkCoordinator_OS_2_11,
                "Factory should use version-based routing for non-CoordinateWorkHttpClient");
        }
    }

    @Test
    void testFactoryThrowsForUnsupportedVersion() {
        // Setup
        var factory = new WorkCoordinatorFactory(Version.fromString("ES 2.0"));  // Very old, unsupported version
        var client = mockOpenSearchHttpClient();

        // Action & Assertions
        assertThrows(IllegalArgumentException.class, () -> {
            factory.get(client, 3600, "test-worker");
        }, "Factory should throw IllegalArgumentException for unsupported version");
    }

    @Test
    void testServerlessCoordinatorWithIndexNameSuffix() throws Exception {
        // Setup
        var indexNameSuffix = "test_suffix";
        var factory = new WorkCoordinatorFactory(Version.fromString("OS 2.11"), indexNameSuffix);
        var client = mockServerlessHttpClient();

        // Action & Assertions
        try (var coordinator = factory.get(client, 3600, "test-worker")) {
            log.info("Successfully created serverless coordinator with suffix");
            assertTrue(coordinator instanceof OpenSearchWorkCoordinator_ES_8_Serverless,
                "Factory should create serverless coordinator when target is ELASTICSEARCH_SERVERLESS");
        }
    }

    @Test
    void testStandardCoordinatorCreatedForOpenSearch() throws Exception {
        // Setup
        var factory = new WorkCoordinatorFactory(Version.fromString("OS 2.11"));
        var client = mockOpenSearchHttpClient();

        // Action & Assertions
        try (var coordinator = factory.get(client, 3600, "test-worker")) {
            log.info("Successfully created standard coordinator for OpenSearch");
            assertTrue(coordinator instanceof OpenSearchWorkCoordinator_OS_2_11,
                "Factory should create standard coordinator for OpenSearch target");
        }
    }

    @Test
    void testFactoryGetFinalIndexName() {
        // Setup
        var indexNameSuffix = "custom";
        var factory = new WorkCoordinatorFactory(Version.fromString("OS 2.11"), indexNameSuffix);

        // Action
        var finalIndexName = factory.getFinalIndexName();

        // Assertions
        log.info("Final index name: {}", finalIndexName);
        assert finalIndexName.contains("custom") : "Final index name should contain suffix";
    }

    // Helper methods

    private CoordinateWorkHttpClient mockServerlessHttpClient() {
        var client = mock(CoordinateWorkHttpClient.class);
        var context = mock(ConnectionContext.class);
        when(context.getTargetType()).thenReturn(ConnectionContext.TargetType.ELASTICSEARCH_SERVERLESS);
        when(client.getConnectionContext()).thenReturn(context);
        return client;
    }

    private CoordinateWorkHttpClient mockOpenSearchHttpClient() {
        var client = mock(CoordinateWorkHttpClient.class);
        var context = mock(ConnectionContext.class);
        when(context.getTargetType()).thenReturn(ConnectionContext.TargetType.OPENSEARCH);
        when(client.getConnectionContext()).thenReturn(context);
        return client;
    }

    private CoordinateWorkHttpClient mockElasticsearchHttpClient() {
        var client = mock(CoordinateWorkHttpClient.class);
        var context = mock(ConnectionContext.class);
        when(context.getTargetType()).thenReturn(ConnectionContext.TargetType.ELASTICSEARCH);
        when(client.getConnectionContext()).thenReturn(context);
        return client;
    }
}

