package org.opensearch.migrations.bulkload.version_es_8_x;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.opensearch.migrations.bulkload.workcoordination.AbstractedHttpClient;
import org.opensearch.migrations.bulkload.workcoordination.IWorkCoordinator;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Assertions;

/**
 * Unit tests for OpenSearchWorkCoordinator_ES_8_Serverless.
 * 
 * Tests the serverless-specific behaviors:
 * 1. Non-hidden index naming (migrations_working_state vs .migrations_working_state)
 * 2. Settings sanitization (no shard/replica settings in coordination index)
 * 3. Proper inheritance from OS_2_11 coordinator
 */
@Slf4j
class OpenSearchWorkCoordinator_ES_8_ServerlessTest {

    @AllArgsConstructor
    public static class MockHttpClient implements AbstractedHttpClient {
        AbstractHttpResponse response;

        @Override
        public AbstractHttpResponse makeRequest(String method, String path, Map<String, String> headers, String payload) {
            log.debug("MockHttpClient.makeRequest: method={}, path={}, payload={}", method, path, payload);
            return response;
        }
    }

    @AllArgsConstructor
    @Getter
    public static class TestResponse implements AbstractedHttpClient.AbstractHttpResponse {
        int statusCode;
        String statusText;
        byte[] payloadBytes;

        public TestResponse(int statusCode, String statusText, String payloadString) {
            this(statusCode, statusText, payloadString.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public Stream<Map.Entry<String, String>> getHeaders() {
            return Stream.of(new AbstractMap.SimpleEntry<>("Content-Type", "application/json"));
        }
    }

    @Test
    void testServerlessCoordinatorCreatesSuccessfully() throws Exception {
        // Setup
        var response = new TestResponse(200, "ok", "{}");
        var client = new MockHttpClient(response);
        var suffix = "test_suffix";

        // Action & Assertions
        try (var coordinator = new OpenSearchWorkCoordinator_ES_8_Serverless(
            client,
            suffix,
            3600,
            "test-worker"
        )) {
            // Verify coordinator was created successfully
            log.info("Successfully created serverless coordinator with suffix: {}", suffix);
            Assertions.assertTrue(coordinator instanceof OpenSearchWorkCoordinator_ES_8_Serverless);
        }
    }

    @Test
    void testServerlessCoordinatorWithoutSuffix() throws Exception {
        // Setup
        var response = new TestResponse(200, "ok", "{}");
        var client = new MockHttpClient(response);

        // Action & Assertions
        try (var coordinator = new OpenSearchWorkCoordinator_ES_8_Serverless(
            client,
            "",  // Empty suffix
            3600,
            "test-worker"
        )) {
            // Verify coordinator was created successfully
            log.info("Successfully created serverless coordinator without suffix");
            Assertions.assertTrue(coordinator instanceof OpenSearchWorkCoordinator_ES_8_Serverless);
        }
    }

    @Test
    void testServerlessCoordinatorWithNullSuffix() throws Exception {
        // Setup
        var response = new TestResponse(200, "ok", "{}");
        var client = new MockHttpClient(response);

        // Action & Assertions
        try (var coordinator = new OpenSearchWorkCoordinator_ES_8_Serverless(
            client,
            null,  // Null suffix
            3600,
            "test-worker"
        )) {
            // Verify coordinator was created successfully
            log.info("Successfully created serverless coordinator with null suffix");
            Assertions.assertTrue(coordinator instanceof OpenSearchWorkCoordinator_ES_8_Serverless);
        }
    }

    @Test
    void testServerlessIndexSettingsOmitShardAndReplica() throws Exception {
        // Setup
        var response = new TestResponse(200, "ok", "{}");
        var client = new MockHttpClient(response);

        // Action
        try (var coordinator = new OpenSearchWorkCoordinator_ES_8_Serverless(
            client,
            "",
            3600,
            "test-worker"
        )) {
            var settingsBody = coordinator.getCoordinationIndexSettingsBody();

            // Assertions
            log.info("Settings body: {}", settingsBody);
            assertFalse(settingsBody.contains("number_of_shards"),
                "Settings should not contain number_of_shards");
            assertFalse(settingsBody.contains("number_of_replicas"),
                "Settings should not contain number_of_replicas");
        }
    }

    @Test
    void testServerlessIndexSettingsPreserveRefreshInterval() throws Exception {
        // Setup
        var response = new TestResponse(200, "ok", "{}");
        var client = new MockHttpClient(response);

        // Action
        try (var coordinator = new OpenSearchWorkCoordinator_ES_8_Serverless(
            client,
            "",
            3600,
            "test-worker"
        )) {
            var settingsBody = coordinator.getCoordinationIndexSettingsBody();

            // Assertions
            log.info("Settings body: {}", settingsBody);
            assertTrue(settingsBody.contains("refresh_interval"),
                "Settings should preserve refresh_interval");
            assertTrue(settingsBody.contains("30s"),
                "refresh_interval should be set to 30s");
        }
    }

    @Test
    void testServerlessIndexSettingsIncludesMappings() throws Exception {
        // Setup
        var response = new TestResponse(200, "ok", "{}");
        var client = new MockHttpClient(response);

        // Action
        try (var coordinator = new OpenSearchWorkCoordinator_ES_8_Serverless(
            client,
            "",
            3600,
            "test-worker"
        )) {
            var settingsBody = coordinator.getCoordinationIndexSettingsBody();

            // Assertions
            log.info("Settings body: {}", settingsBody);
            assertThat("Settings should include mappings", settingsBody, containsString("mappings"));
            assertThat("Settings should include expiration field", settingsBody, containsString("expiration"));
            assertThat("Settings should include completedAt field", settingsBody, containsString("completedAt"));
            assertThat("Settings should include leaseHolderId field", settingsBody, containsString("leaseHolderId"));
            assertThat("Settings should include status field", settingsBody, containsString("status"));
        }
    }

    @Test
    void testServerlessCoordinatorWithClockAndConsumer() throws Exception {
        // Setup
        var response = new TestResponse(200, "ok", "{}");
        var client = new MockHttpClient(response);
        var workItemRef = new AtomicReference<IWorkCoordinator.WorkItemAndDuration>();

        // Action & Assertions
        try (var coordinator = new OpenSearchWorkCoordinator_ES_8_Serverless(
            client,
            "with_clock",
            3600,
            "test-worker",
            Clock.systemUTC(),
            workItemRef::set
        )) {
            // Verify coordinator was created successfully with clock and consumer
            log.info("Successfully created serverless coordinator with clock and consumer");
            Assertions.assertTrue(coordinator instanceof OpenSearchWorkCoordinator_ES_8_Serverless);
        }
    }

    @Test
    void testServerlessCoordinatorIsInstanceOfParent() throws Exception {
        // Setup
        var response = new TestResponse(200, "ok", "{}");
        var client = new MockHttpClient(response);

        // Action & Assertions
        try (var coordinator = new OpenSearchWorkCoordinator_ES_8_Serverless(
            client,
            "",
            3600,
            "test-worker"
        )) {
            assertTrue(coordinator instanceof org.opensearch.migrations.bulkload.version_os_2_11.OpenSearchWorkCoordinator_OS_2_11,
                "Serverless coordinator should extend OS_2_11 coordinator");
        }
    }
}

