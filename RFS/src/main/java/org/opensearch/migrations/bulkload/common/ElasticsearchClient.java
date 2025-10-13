package org.opensearch.migrations.bulkload.common;

import org.opensearch.migrations.Version;
import org.opensearch.migrations.bulkload.common.http.CompressionMode;
import org.opensearch.migrations.bulkload.common.http.ConnectionContext;
import org.opensearch.migrations.reindexer.FailedRequestsLogger;

/**
 * Elasticsearch client implementation that extends OpenSearchClient
 * to handle Elasticsearch-specific API differences and serverless constraints
 */
public class ElasticsearchClient extends OpenSearchClient {

    public ElasticsearchClient(ConnectionContext connectionContext, Version version, CompressionMode compressionMode) {
        super(connectionContext, version, compressionMode);
    }

    public ElasticsearchClient(RestClient client, FailedRequestsLogger failedRequestsLogger, Version version, CompressionMode compressionMode) {
        super(client, failedRequestsLogger, version, compressionMode);
    }

    @Override
    protected String getCreateIndexPath(String indexName) {
        // Elasticsearch uses the same create index path as OpenSearch
        return indexName;
    }

    @Override
    protected String getBulkRequestPath(String indexName) {
        // Elasticsearch uses the same bulk request path as OpenSearch
        return "_bulk";
    }

    /**
     * Sanitize index settings for Elasticsearch compatibility
     * Removes OpenSearch-specific settings and handles serverless constraints
     */
    public void sanitizeIndexSettingsForElasticsearch(com.fasterxml.jackson.databind.node.ObjectNode settings) {
        // Remove OpenSearch-specific settings
        settings.remove("plugins");
        settings.remove("opensearch");
        
        // Handle serverless constraints - these would need to be passed from the connection context
        // For now, we'll implement basic sanitization
        settings.remove("number_of_shards");
        settings.remove("number_of_replicas");
    }

    /**
     * Sanitize index template for Elasticsearch compatibility
     */
    public void sanitizeTemplateForElasticsearch(com.fasterxml.jackson.databind.node.ObjectNode template) {
        // Remove OpenSearch-specific template settings
        template.remove("opensearch");
    }

    /**
     * Sanitize index template for Elasticsearch compatibility (alias for consistency)
     */
    public void sanitizeIndexTemplateForElasticsearch(com.fasterxml.jackson.databind.node.ObjectNode template) {
        sanitizeTemplateForElasticsearch(template);
    }

    /**
     * Override getAwarenessAttributeSettings for Elasticsearch Serverless compatibility
     * Serverless doesn't support cluster settings API, so we return default values
     */
    @Override
    public org.opensearch.migrations.AwarenessAttributeSettings getAwarenessAttributeSettings() {
        // For Elasticsearch Serverless, return default awareness settings
        // since cluster management APIs are not available
        return org.opensearch.migrations.AwarenessAttributeSettings.builder()
            .build();
    }
}