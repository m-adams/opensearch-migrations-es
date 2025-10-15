package org.opensearch.migrations.bulkload.version_es_8_x;

import java.time.Clock;
import java.util.Optional;
import java.util.function.Consumer;

import org.opensearch.migrations.bulkload.version_os_2_11.OpenSearchWorkCoordinator_OS_2_11;
import org.opensearch.migrations.bulkload.workcoordination.AbstractedHttpClient;

/**
 * WorkCoordinator for Elasticsearch Serverless that handles two key differences:
 * 
 * 1. Index name: Serverless doesn't allow hidden indices (starting with dot),
 *    so we use "migrations_working_state" instead of ".migrations_working_state"
 * 
 * 2. Index settings: Omits number_of_shards and number_of_replicas since
 *    Elasticsearch Serverless automatically manages these.
 */
public class OpenSearchWorkCoordinator_ES_8_Serverless extends OpenSearchWorkCoordinator_OS_2_11 {
    
    // Elasticsearch Serverless doesn't allow index names starting with "."
    private static final String SERVERLESS_INDEX_BASENAME = "migrations_working_state";
    
    public OpenSearchWorkCoordinator_ES_8_Serverless(
        AbstractedHttpClient httpClient,
        String indexNameSuffix,
        long tolerableClientServerClockDifferenceSeconds,
        String workerId
    ) {
        super(httpClient, indexNameSuffix, tolerableClientServerClockDifferenceSeconds, workerId);
        // Override the index name to remove the leading dot
        setIndexName(buildServerlessIndexName(indexNameSuffix));
    }

    public OpenSearchWorkCoordinator_ES_8_Serverless(
        AbstractedHttpClient httpClient,
        String indexNameSuffix,
        long tolerableClientServerClockDifferenceSeconds,
        String workerId,
        Clock clock,
        Consumer<WorkItemAndDuration> workItemConsumer
    ) {
        super(httpClient, indexNameSuffix, tolerableClientServerClockDifferenceSeconds, workerId, clock, workItemConsumer);
        // Override the index name to remove the leading dot
        setIndexName(buildServerlessIndexName(indexNameSuffix));
    }

    /**
     * Build serverless-compatible index name without the leading dot.
     */
    private static String buildServerlessIndexName(String suffix) {
        return SERVERLESS_INDEX_BASENAME + Optional.ofNullable(suffix)
            .filter(s -> !s.isEmpty())
            .map(s -> "_" + s)
            .orElse("");
    }

    /**
     * Override to exclude index settings that are not available in Elasticsearch Serverless.
     * Serverless automatically manages shards and replicas.
     */
    @Override
    protected String getCoordinationIndexSettingsBody() {
        return "{\n"
            + "  \"settings\": {\n"
            + "   \"index\": {"
            + "    \"refresh_interval\": \"30s\"\n"
            + "   }\n"
            + "  },\n"
            + "  \"mappings\": {\n"
            + "    \"properties\": {\n"
            + "      \"" + EXPIRATION_FIELD_NAME + "\": {\n"
            + "        \"type\": \"long\"\n"
            + "      },\n"
            + "      \"" + COMPLETED_AT_FIELD_NAME + "\": {\n"
            + "        \"type\": \"long\"\n"
            + "      },\n"
            + "      \"leaseHolderId\": {\n"
            + "        \"type\": \"keyword\",\n"
            + "        \"norms\": false\n"
            + "      },\n"
            + "      \"status\": {\n"
            + "        \"type\": \"keyword\",\n"
            + "        \"norms\": false\n"
            + "      },\n"
            + "     \"" + SUCCESSOR_ITEMS_FIELD_NAME + "\": {\n"
            + "       \"type\": \"keyword\",\n"
            + "        \"norms\": false\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";
    }
}
