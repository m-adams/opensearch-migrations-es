package org.opensearch.migrations.bulkload.version_es_8_x;

import org.opensearch.migrations.AwarenessAttributeSettings;
import org.opensearch.migrations.MigrationMode;
import org.opensearch.migrations.bulkload.common.ElasticsearchClient;
import org.opensearch.migrations.bulkload.common.http.ConnectionContext;
import org.opensearch.migrations.bulkload.models.IndexMetadata;
import org.opensearch.migrations.bulkload.version_os_2_11.IndexCreator_OS_2_11;
import org.opensearch.migrations.bulkload.version_os_2_11.IndexMetadataData_OS_2_11;
import org.opensearch.migrations.metadata.CreationResult;
import org.opensearch.migrations.metadata.tracing.IMetadataMigrationContexts.ICreateIndexContext;
import org.opensearch.migrations.parsing.ObjectNodeUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Index creator for Elasticsearch 8.x and Serverless
 * Extends the OS 2.11 creator but adds Elasticsearch-specific sanitization
 */
@Slf4j
public class IndexCreator_ES_8_X extends IndexCreator_OS_2_11 {

    private final ElasticsearchClient esClient;

    public IndexCreator_ES_8_X(ElasticsearchClient client) {
        super(client);
        this.esClient = client;
    }

    @Override
    public CreationResult create(
        IndexMetadata index,
        MigrationMode mode,
        AwarenessAttributeSettings awarenessAttributeSettings,
        ICreateIndexContext context
    ) {
        // Check if this is Elasticsearch Serverless
        if (esClient.getConnectionContext().getTargetType() == ConnectionContext.TargetType.ELASTICSEARCH_SERVERLESS) {
            log.debug("Applying serverless sanitization for index '{}'", index.getName());
            return createForServerless(index, mode, awarenessAttributeSettings, context);
        }
        
        // For standard Elasticsearch, use parent implementation
        return super.create(index, mode, awarenessAttributeSettings, context);
    }

    /**
     * Create index with serverless-specific sanitization.
     * Removes settings that are not allowed in Elasticsearch Serverless.
     */
    private CreationResult createForServerless(
        IndexMetadata index,
        MigrationMode mode,
        AwarenessAttributeSettings awarenessAttributeSettings,
        ICreateIndexContext context
    ) {
        var result = CreationResult.builder().name(index.getName());
        IndexMetadataData_OS_2_11 indexMetadata = new IndexMetadataData_OS_2_11(index.getRawJson(), index.getId(), index.getName());

        // Get settings and apply both standard and serverless-specific sanitization
        var settings = indexMetadata.getSettings();
        
        // Standard problem settings (from parent class)
        String[] problemSettings = { 
            "creation_date", "provided_name", "uuid", "version", 
            "index.mapping.single_type", "index.mapper.dynamic" 
        };
        for (var field : problemSettings) {
            ObjectNodeUtils.removeFieldsByPath(settings, field);
        }
        
        // Serverless-specific forbidden settings
        // Elasticsearch Serverless manages these automatically
        String[] serverlessForbiddenSettings = {
            "number_of_shards",
            "number_of_replicas", 
            "auto_expand_replicas",
            "shard.check_on_startup",
            "routing.allocation.enable",
            "routing.rebalance.enable",
            "codec"
        };
        
        for (var field : serverlessForbiddenSettings) {
            ObjectNodeUtils.removeFieldsByPath(settings, field);
            log.debug("Removing serverless-forbidden setting '{}' from index '{}' (if present)", field, index.getName());
        }
        
        log.info("Applied serverless sanitization to index '{}'", index.getName());

        // Sanitize mappings
        var mappings = indexMetadata.getMappings();
        String[] problemMappingFields = { "_all" };
        for (var field : problemMappingFields) {
            ObjectNodeUtils.removeFieldsByPath(mappings, field);
        }

        // Assemble request body
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var body = mapper.createObjectNode();
        body.set("aliases", indexMetadata.getAliases());
        body.set("mappings", mappings);
        body.set("settings", settings);

        try {
            // Use the parent's createInner logic via reflection or directly create the index
            // Since createInner is private, we'll duplicate the simple case here
            var alreadyExists = false;
            if (mode == MigrationMode.SIMULATE) {
                alreadyExists = esClient.hasIndex(index.getName());
            } else if (mode == MigrationMode.PERFORM) {
                alreadyExists = esClient.createIndex(index.getName(), body, context).isEmpty();
            }

            if (alreadyExists) {
                result.failureType(CreationResult.CreationFailureType.ALREADY_EXISTS);
            }
            
            // Skip replica count check for serverless (it doesn't apply)
            
        } catch (Exception e) {
            log.error("Failed to create index '{}' in serverless: {}", index.getName(), e.getMessage());
            result.failureType(CreationResult.CreationFailureType.TARGET_CLUSTER_FAILURE);
            result.exception(e);
        }
        
        return result.build();
    }
}
