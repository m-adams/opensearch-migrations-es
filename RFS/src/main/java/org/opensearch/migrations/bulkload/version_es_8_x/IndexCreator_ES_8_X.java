package org.opensearch.migrations.bulkload.version_es_8_x;

import org.opensearch.migrations.bulkload.common.ElasticsearchClient;
import org.opensearch.migrations.bulkload.version_os_2_11.IndexCreator_OS_2_11;

/**
 * Index creator for Elasticsearch 8.x and Serverless
 * Extends the OS 2.11 creator but adds Elasticsearch-specific sanitization
 */
public class IndexCreator_ES_8_X extends IndexCreator_OS_2_11 {

    public IndexCreator_ES_8_X(ElasticsearchClient client) {
        super(client);
    }

    // For now, we'll use the parent implementation
    // TODO: Add Elasticsearch-specific sanitization methods when needed
}
