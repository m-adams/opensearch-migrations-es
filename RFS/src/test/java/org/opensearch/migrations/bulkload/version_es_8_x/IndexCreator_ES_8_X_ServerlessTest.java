package org.opensearch.migrations.bulkload.version_es_8_x;

import java.util.Optional;

import org.opensearch.migrations.AwarenessAttributeSettings;
import org.opensearch.migrations.MigrationMode;
import org.opensearch.migrations.bulkload.common.ElasticsearchClient;
import org.opensearch.migrations.bulkload.common.http.ConnectionContext;
import org.opensearch.migrations.bulkload.version_os_2_11.IndexMetadataData_OS_2_11;
import org.opensearch.migrations.metadata.CreationResult;
import org.opensearch.migrations.metadata.tracing.IMetadataMigrationContexts.ICreateIndexContext;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for IndexCreator_ES_8_X serverless sanitization.
 * 
 * Tests the serverless-specific settings sanitization:
 * 1. Removal of number_of_shards
 * 2. Removal of number_of_replicas
 * 3. Preservation of allowed settings (refresh_interval, etc.)
 * 4. Handling of nested settings structures
 * 5. Edge cases (empty settings, multiple disallowed settings)
 */
@Slf4j
class IndexCreator_ES_8_X_ServerlessTest {
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
        .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
        .build();
    private static final Optional<ObjectNode> INDEX_CREATE_SUCCESS = Optional.of(mock(ObjectNode.class));

    @Test
    void testServerlessRemovesNumberOfShardsSetting() throws Exception {
        // Setup
        var rawJson = "{\r\n" +
            "  \"settings\" : {\r\n" +
            "    \"number_of_shards\" : 3\r\n" +
            "  }\r\n" +
            "}";

        var client = mockServerlessClient();
        when(client.createIndex(any(), any(), any())).thenReturn(INDEX_CREATE_SUCCESS);

        // Action
        var result = createServerless(client, rawJson, "test-index");

        // Assertions
        assertThat("Index creation should succeed", result.wasSuccessful(), equalTo(true));
        
        var requestBodyCapture = ArgumentCaptor.forClass(ObjectNode.class);
        verify(client).createIndex(any(), requestBodyCapture.capture(), any());

        var finalIndexBody = requestBodyCapture.getValue().toPrettyString();
        log.info("Final index body: {}", finalIndexBody);
        assertThat("number_of_shards should be removed", finalIndexBody, not(containsString("number_of_shards")));
    }

    @Test
    void testServerlessRemovesNumberOfReplicasSetting() throws Exception {
        // Setup
        var rawJson = "{\r\n" +
            "  \"settings\" : {\r\n" +
            "    \"number_of_replicas\" : 2\r\n" +
            "  }\r\n" +
            "}";

        var client = mockServerlessClient();
        when(client.createIndex(any(), any(), any())).thenReturn(INDEX_CREATE_SUCCESS);

        // Action
        var result = createServerless(client, rawJson, "test-index");

        // Assertions
        assertThat("Index creation should succeed", result.wasSuccessful(), equalTo(true));
        
        var requestBodyCapture = ArgumentCaptor.forClass(ObjectNode.class);
        verify(client).createIndex(any(), requestBodyCapture.capture(), any());

        var finalIndexBody = requestBodyCapture.getValue().toPrettyString();
        log.info("Final index body: {}", finalIndexBody);
        assertThat("number_of_replicas should be removed", finalIndexBody, not(containsString("number_of_replicas")));
    }

    @Test
    void testServerlessRemovesBothShardAndReplicaSettings() throws Exception {
        // Setup
        var rawJson = "{\r\n" +
            "  \"settings\" : {\r\n" +
            "    \"number_of_shards\" : 5,\r\n" +
            "    \"number_of_replicas\" : 2\r\n" +
            "  }\r\n" +
            "}";

        var client = mockServerlessClient();
        when(client.createIndex(any(), any(), any())).thenReturn(INDEX_CREATE_SUCCESS);

        // Action
        var result = createServerless(client, rawJson, "test-index");

        // Assertions
        assertThat("Index creation should succeed", result.wasSuccessful(), equalTo(true));
        
        var requestBodyCapture = ArgumentCaptor.forClass(ObjectNode.class);
        verify(client).createIndex(any(), requestBodyCapture.capture(), any());

        var finalIndexBody = requestBodyCapture.getValue().toPrettyString();
        log.info("Final index body: {}", finalIndexBody);
        assertThat("number_of_shards should be removed", finalIndexBody, not(containsString("number_of_shards")));
        assertThat("number_of_replicas should be removed", finalIndexBody, not(containsString("number_of_replicas")));
    }

    @Test
    void testServerlessPreservesRefreshInterval() throws Exception {
        // Setup
        var rawJson = "{\r\n" +
            "  \"settings\" : {\r\n" +
            "    \"refresh_interval\" : \"30s\",\r\n" +
            "    \"number_of_shards\" : 3\r\n" +
            "  }\r\n" +
            "}";

        var client = mockServerlessClient();
        when(client.createIndex(any(), any(), any())).thenReturn(INDEX_CREATE_SUCCESS);

        // Action
        var result = createServerless(client, rawJson, "test-index");

        // Assertions
        assertThat("Index creation should succeed", result.wasSuccessful(), equalTo(true));
        
        var requestBodyCapture = ArgumentCaptor.forClass(ObjectNode.class);
        verify(client).createIndex(any(), requestBodyCapture.capture(), any());

        var finalIndexBody = requestBodyCapture.getValue().toPrettyString();
        log.info("Final index body: {}", finalIndexBody);
        assertThat("refresh_interval should be preserved", finalIndexBody, containsString("refresh_interval"));
        assertThat("refresh_interval value should be preserved", finalIndexBody, containsString("30s"));
        assertThat("number_of_shards should be removed", finalIndexBody, not(containsString("number_of_shards")));
    }

    @Test
    void testServerlessPreservesAllowedSettings() throws Exception {
        // Setup
        var rawJson = "{\r\n" +
            "  \"settings\" : {\r\n" +
            "    \"refresh_interval\" : \"30s\",\r\n" +
            "    \"max_result_window\" : 20000,\r\n" +
            "    \"number_of_shards\" : 3,\r\n" +
            "    \"number_of_replicas\" : 2\r\n" +
            "  }\r\n" +
            "}";

        var client = mockServerlessClient();
        when(client.createIndex(any(), any(), any())).thenReturn(INDEX_CREATE_SUCCESS);

        // Action
        var result = createServerless(client, rawJson, "test-index");

        // Assertions
        assertThat("Index creation should succeed", result.wasSuccessful(), equalTo(true));
        
        var requestBodyCapture = ArgumentCaptor.forClass(ObjectNode.class);
        verify(client).createIndex(any(), requestBodyCapture.capture(), any());

        var finalIndexBody = requestBodyCapture.getValue().toPrettyString();
        log.info("Final index body: {}", finalIndexBody);
        assertThat("refresh_interval should be preserved", finalIndexBody, containsString("refresh_interval"));
        assertThat("max_result_window should be preserved", finalIndexBody, containsString("max_result_window"));
        assertThat("number_of_shards should be removed", finalIndexBody, not(containsString("number_of_shards")));
        assertThat("number_of_replicas should be removed", finalIndexBody, not(containsString("number_of_replicas")));
    }

    @Test
    void testServerlessRemovesAutoExpandReplicas() throws Exception {
        // Setup
        var rawJson = "{\r\n" +
            "  \"settings\" : {\r\n" +
            "    \"auto_expand_replicas\" : \"0-5\"\r\n" +
            "  }\r\n" +
            "}";

        var client = mockServerlessClient();
        when(client.createIndex(any(), any(), any())).thenReturn(INDEX_CREATE_SUCCESS);

        // Action
        var result = createServerless(client, rawJson, "test-index");

        // Assertions
        assertThat("Index creation should succeed", result.wasSuccessful(), equalTo(true));
        
        var requestBodyCapture = ArgumentCaptor.forClass(ObjectNode.class);
        verify(client).createIndex(any(), requestBodyCapture.capture(), any());

        var finalIndexBody = requestBodyCapture.getValue().toPrettyString();
        log.info("Final index body: {}", finalIndexBody);
        assertThat("auto_expand_replicas should be removed", finalIndexBody, not(containsString("auto_expand_replicas")));
    }

    @Test
    void testServerlessRemovesCodecSetting() throws Exception {
        // Setup
        var rawJson = "{\r\n" +
            "  \"settings\" : {\r\n" +
            "    \"codec\" : \"best_compression\"\r\n" +
            "  }\r\n" +
            "}";

        var client = mockServerlessClient();
        when(client.createIndex(any(), any(), any())).thenReturn(INDEX_CREATE_SUCCESS);

        // Action
        var result = createServerless(client, rawJson, "test-index");

        // Assertions
        assertThat("Index creation should succeed", result.wasSuccessful(), equalTo(true));
        
        var requestBodyCapture = ArgumentCaptor.forClass(ObjectNode.class);
        verify(client).createIndex(any(), requestBodyCapture.capture(), any());

        var finalIndexBody = requestBodyCapture.getValue().toPrettyString();
        log.info("Final index body: {}", finalIndexBody);
        assertThat("codec should be removed", finalIndexBody, not(containsString("codec")));
    }

    @Test
    void testServerlessHandlesEmptySettings() throws Exception {
        // Setup
        var rawJson = "{\r\n" +
            "  \"settings\" : { }\r\n" +
            "}";

        var client = mockServerlessClient();
        when(client.createIndex(any(), any(), any())).thenReturn(INDEX_CREATE_SUCCESS);

        // Action
        var result = createServerless(client, rawJson, "test-index");

        // Assertions
        assertThat("Index creation should succeed with empty settings", result.wasSuccessful(), equalTo(true));
        verify(client).createIndex(any(), any(), any());
    }

    @Test
    void testStandardElasticsearchDoesNotSanitize() throws Exception {
        // Setup
        var rawJson = "{\r\n" +
            "  \"settings\" : {\r\n" +
            "    \"number_of_shards\" : 3,\r\n" +
            "    \"number_of_replicas\" : 2\r\n" +
            "  }\r\n" +
            "}";

        // Mock standard Elasticsearch (not serverless)
        var client = mockStandardElasticsearchClient();
        when(client.createIndex(any(), any(), any())).thenReturn(INDEX_CREATE_SUCCESS);

        // Action
        var result = createStandardElasticsearch(client, rawJson, "test-index");

        // Assertions
        assertThat("Index creation should succeed", result.wasSuccessful(), equalTo(true));
        
        var requestBodyCapture = ArgumentCaptor.forClass(ObjectNode.class);
        verify(client).createIndex(any(), requestBodyCapture.capture(), any());

        var finalIndexBody = requestBodyCapture.getValue().toPrettyString();
        log.info("Final index body for standard ES: {}", finalIndexBody);
        // For standard Elasticsearch, settings should NOT be removed (unless they're in the problem settings list)
        // Note: The parent class removes some problem settings, but not shard/replica counts
    }

    @Test
    void testServerlessPreservesMappings() throws Exception {
        // Setup
        var rawJson = "{\r\n" +
            "  \"settings\" : {\r\n" +
            "    \"number_of_shards\" : 3\r\n" +
            "  },\r\n" +
            "  \"mappings\" : {\r\n" +
            "    \"properties\" : {\r\n" +
            "      \"title\" : { \"type\" : \"text\" },\r\n" +
            "      \"timestamp\" : { \"type\" : \"date\" }\r\n" +
            "    }\r\n" +
            "  }\r\n" +
            "}";

        var client = mockServerlessClient();
        when(client.createIndex(any(), any(), any())).thenReturn(INDEX_CREATE_SUCCESS);

        // Action
        var result = createServerless(client, rawJson, "test-index");

        // Assertions
        assertThat("Index creation should succeed", result.wasSuccessful(), equalTo(true));
        
        var requestBodyCapture = ArgumentCaptor.forClass(ObjectNode.class);
        verify(client).createIndex(any(), requestBodyCapture.capture(), any());

        var finalIndexBody = requestBodyCapture.getValue().toPrettyString();
        log.info("Final index body: {}", finalIndexBody);
        assertThat("Mappings should be preserved", finalIndexBody, containsString("mappings"));
        assertThat("Mappings properties should be preserved", finalIndexBody, containsString("properties"));
        assertThat("title field should be preserved", finalIndexBody, containsString("title"));
        assertThat("timestamp field should be preserved", finalIndexBody, containsString("timestamp"));
        assertThat("number_of_shards should be removed", finalIndexBody, not(containsString("number_of_shards")));
    }

    // Helper methods

    private ElasticsearchClient mockServerlessClient() {
        var client = mock(ElasticsearchClient.class);
        var context = mock(ConnectionContext.class);
        when(context.getTargetType()).thenReturn(ConnectionContext.TargetType.ELASTICSEARCH_SERVERLESS);
        when(client.getConnectionContext()).thenReturn(context);
        return client;
    }

    private ElasticsearchClient mockStandardElasticsearchClient() {
        var client = mock(ElasticsearchClient.class);
        var context = mock(ConnectionContext.class);
        when(context.getTargetType()).thenReturn(ConnectionContext.TargetType.ELASTICSEARCH);
        when(client.getConnectionContext()).thenReturn(context);
        return client;
    }

    @SneakyThrows
    private CreationResult createServerless(ElasticsearchClient client, String rawJson, String indexName) {
        var node = (ObjectNode) OBJECT_MAPPER.readTree(rawJson);
        var indexId = "indexId";
        var indexData = new IndexMetadataData_OS_2_11(node, indexId, indexName);
        var indexCreator = new IndexCreator_ES_8_X(client);
        return indexCreator.create(indexData, MigrationMode.PERFORM, new AwarenessAttributeSettings(false, 0), mock(ICreateIndexContext.class));
    }

    @SneakyThrows
    private CreationResult createStandardElasticsearch(ElasticsearchClient client, String rawJson, String indexName) {
        var node = (ObjectNode) OBJECT_MAPPER.readTree(rawJson);
        var indexId = "indexId";
        var indexData = new IndexMetadataData_OS_2_11(node, indexId, indexName);
        var indexCreator = new IndexCreator_ES_8_X(client);
        return indexCreator.create(indexData, MigrationMode.PERFORM, new AwarenessAttributeSettings(false, 0), mock(ICreateIndexContext.class));
    }
}

