package org.opensearch.migrations.bulkload.version_es_8_x;

import org.opensearch.migrations.Version;
import org.opensearch.migrations.VersionMatchers;
import org.opensearch.migrations.bulkload.common.ElasticsearchClient;
import org.opensearch.migrations.bulkload.common.OpenSearchClientFactory;
import org.opensearch.migrations.bulkload.common.http.ConnectionContext;
import org.opensearch.migrations.bulkload.models.DataFilterArgs;
import org.opensearch.migrations.bulkload.version_os_2_11.GlobalMetadataCreator_OS_2_11;
import org.opensearch.migrations.bulkload.version_os_2_11.IndexCreator_OS_2_11;
import org.opensearch.migrations.cluster.ClusterWriter;
import org.opensearch.migrations.cluster.RemoteCluster;
import org.opensearch.migrations.metadata.GlobalMetadataCreator;
import org.opensearch.migrations.metadata.IndexCreator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RemoteWriter_ES_8_X implements RemoteCluster, ClusterWriter {
    private Version version;
    private ElasticsearchClient client;
    private ConnectionContext connection;
    private DataFilterArgs dataFilterArgs;

    @Override
    public boolean compatibleWith(Version version) {
        // Support Elasticsearch 8.x and Elasticsearch Serverless
        return VersionMatchers.isES_8_X.test(version) || 
               version.getFlavor().name().contains("ELASTICSEARCH");
    }

    @Override
    public boolean looseCompatibleWith(Version version) {
        // For serverless, be more lenient with version matching
        return this.compatibleWith(version) || 
               version.getFlavor().name().contains("ELASTICSEARCH");
    }

    @Override
    public ClusterWriter initialize(Version versionOverride) {
        if (versionOverride != null) {
            log.warn("Overriding version for cluster, " + versionOverride);
            this.version = versionOverride;
        }
        return this;
    }

    @Override
    public ClusterWriter initialize(DataFilterArgs dataFilterArgs) {
        this.dataFilterArgs = dataFilterArgs;
        return this;
    }

    @Override
    public RemoteCluster initialize(ConnectionContext connection) {
        this.connection = connection;
        var clientFactory = new OpenSearchClientFactory(connection);
        var createdClient = clientFactory.determineVersionAndCreate();
        
        // Handle both ElasticsearchClient and OpenSearchClient types
        if (createdClient instanceof ElasticsearchClient) {
            this.client = (ElasticsearchClient) createdClient;
        } else {
            // If it's not an ElasticsearchClient, we need to create one
            // This can happen if the version detection doesn't work as expected
            log.warn("Expected ElasticsearchClient but got: " + createdClient.getClass().getSimpleName());
            // For now, we'll work with the OpenSearchClient and cast it later
            this.client = null; // We'll handle this in getClient()
        }
        return this;
    }

    @Override
    public GlobalMetadataCreator getGlobalMetadataCreator() {
        return new GlobalMetadataCreator_OS_2_11(
            getClient(),
            getDataFilterArgs().indexTemplateAllowlist,
            getDataFilterArgs().componentTemplateAllowlist,
            getDataFilterArgs().indexTemplateAllowlist);
    }

    @Override
    public IndexCreator getIndexCreator() {
        return new IndexCreator_ES_8_X(getClient());
    }

    @Override
    public Version getVersion() {
        if (version == null) {
            version = getClient().getClusterVersion();
        }
        return version;
    }

    @Override
    public org.opensearch.migrations.AwarenessAttributeSettings getAwarenessAttributeSettings() {
        return getClient().getAwarenessAttributeSettings();
    }

    @Override
    public String getFriendlyTypeName() {
        return "Elasticsearch 8.x/Serverless";
    }

    @Override
    public ConnectionContext getConnection() {
        return connection;
    }

    private ElasticsearchClient getClient() {
        if (client == null) {
            // For serverless targets, create ElasticsearchClient directly
            if (connection.getTargetType() == ConnectionContext.TargetType.ELASTICSEARCH_SERVERLESS) {
                // Create a version for Elasticsearch Serverless
                var version = Version.builder()
                    .flavor(org.opensearch.migrations.Flavor.ELASTICSEARCH)
                    .major(8)
                    .minor(11)
                    .build();
                client = new ElasticsearchClient(connection, version, org.opensearch.migrations.bulkload.common.http.CompressionMode.UNCOMPRESSED);
            } else {
                // For other targets, use the factory
                var clientFactory = new OpenSearchClientFactory(connection);
                var createdClient = clientFactory.determineVersionAndCreate();
                if (createdClient instanceof ElasticsearchClient) {
                    client = (ElasticsearchClient) createdClient;
                } else {
                    // Fallback: create ElasticsearchClient
                    var version = clientFactory.getClusterVersion();
                    client = new ElasticsearchClient(connection, version, org.opensearch.migrations.bulkload.common.http.CompressionMode.UNCOMPRESSED);
                }
            }
        }
        return client;
    }

    private DataFilterArgs getDataFilterArgs() {
        return dataFilterArgs;
    }
}
