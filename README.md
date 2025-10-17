# Migrate from OpenSearch to Elasticsearch Serverless

**A production-ready fork of OpenSearch Migration Assistant, extended specifically for Elasticsearch Serverless on Elastic Cloud.**

## Table of Contents
- [Migrate from OpenSearch to Elasticsearch Serverless](#migrate-from-opensearch-to-elasticsearch-serverless)
  - [Table of Contents](#table-of-contents)
  - [Overview](#overview)
  - [Fork Highlights](#fork-highlights)
  - [Key Features](#key-features)
  - [Supported Migration Paths and Platforms](#supported-migration-paths-and-platforms)
    - [Migration Paths](#migration-paths)
    - [Platforms](#platforms)
    - [Performance Limitations](#performance-limitations)
      - [Test Results](#test-results)
  - [Migrating to Elasticsearch Serverless](#migrating-to-elasticsearch-serverless)
  - [Issue Tracking](#issue-tracking)
  - [User Guide Documentation](#user-guide-documentation)
  - [Getting Started](#getting-started)
    - [Local Deployment](#local-deployment)
    - [AWS Deployment](#aws-deployment)
    - [Elasticsearch Serverless Migration](#elasticsearch-serverless-migration)
  - [Continuous Integration and Deployment](#continuous-integration-and-deployment)
  - [Contributing](#contributing)
  - [Security](#security)
  - [License](#license)
  - [Acknowledgments](#acknowledgments)


## Overview

**This is a fork of the [OpenSearch Migration Assistant](https://github.com/opensearch-project/opensearch-migrations) extended specifically for migrating to Elasticsearch Serverless on Elastic Cloud.**

### Why Elasticsearch Serverless?

Moving from OpenSearch to Elasticsearch Serverless isn't just a platform change—it's a fundamental shift in how you operate search infrastructure:

**What disappears:**
- Shard count calculations
- Replica factor tuning
- Node sizing decisions
- Capacity planning
- Manual scaling operations
- Infrastructure patching

**What you get:**
- Automatic scaling that responds to actual demand
- Zero infrastructure management
- Pay-per-use economics (scales to zero when idle)
- Enterprise security and compliance built-in
- Modern vector search with [Better Binary Quantization (BBQ)](https://www.elastic.co/docs/reference/elasticsearch/index-settings/bbq)

**For AWS users:** Elasticsearch Serverless is available through the [AWS Marketplace](https://aws.amazon.com/marketplace/seller-profile?id=4bd95219-3608-4318-8590-e86aface431e), making procurement seamless within your existing AWS relationship.

**Note:** This toolkit also supports Elastic Cloud hosted clusters, though those still require infrastructure management similar to OpenSearch Service (shards, replicas, capacity planning). The real operational transformation comes with Serverless.

OpenSearch Migration Assistant is a comprehensive set of tools designed to facilitate upgrades, migrations, and comparisons for OpenSearch and Elasticsearch clusters. This project aims to simplify the process of moving between different versions and platforms while ensuring data integrity and performance.

## Fork Highlights

This fork extends the upstream opensearch-migrations project with:

✅ **Elasticsearch Serverless Support**
- Migrate from AWS OpenSearch to Elasticsearch Serverless (Elastic Cloud)
- Automatic settings sanitization for serverless compatibility
- Work coordination index adaptation (removes hidden index restrictions)
- API restriction handling (cluster settings, node APIs, etc.)

✅ **Minimal, Maintainable Changes**
- ~120 lines of code across 3 files
- Clean separation from upstream code
- Easy to merge with upstream updates
- Follows existing version-specific patterns

✅ **Production-Ready**
- Tested with real Elasticsearch Serverless deployments
- Comprehensive documentation and troubleshooting guides
- Support for both API key and basic authentication

📚 **Additional Documentation**
- [Elasticsearch Serverless Migration Guide](DocumentsFromSnapshotMigration/ELASTICSEARCH_SERVERLESS.md)
- [Serverless Implementation Details](../docs/serverless-implementation.md) (in parent repo)
- [Serverless Compatibility Matrix](../docs/serverless-compatibility-matrix.md) (in parent repo)

🔗 **Upstream Project**: [opensearch-project/opensearch-migrations](https://github.com/opensearch-project/opensearch-migrations)

## Key Features

- **Upgrade and Migration Support**: Provides tools for migrating between different versions of Elasticsearch and OpenSearch.
  - **[Metadata Migration](MetadataMigration/README.md)**: Migrate essential cluster components such as configuration, settings, templates, and aliases.
  - **Multi-Version Upgrade**: Easily migrate across major versions (e.g., from Elasticsearch 6.8 to OpenSearch 2.15), skipping intermediate upgrades and reducing time and risk.
  - **Downgrade Support**: Downgrade to an earlier version if needed (e.g., from Elasticsearch 7.17 to 7.10.2).
  - **Existing Data Migration with [Reindex-from-Snapshot](RFS/docs/DESIGN.md)**: Migrate indices and documents using snapshots, updating your data to the latest Lucene version quickly without impacting the target cluster.
  - **Live Traffic Capture with [Capture-and-Replay](docs/TrafficCaptureAndReplayDesign.md)**: Capture live traffic from the source cluster and replay it on the target cluster for validation. This ensures the target cluster can handle real-world traffic patterns before fully migrating.

- **Elasticsearch Serverless Support** ⭐ _(Fork Extension)_:
  - **Automatic Settings Sanitization**: Removes forbidden settings (shards, replicas, codec, translog, merge policies) that aren't supported in serverless mode.
  - **Hidden Index Adaptation**: Converts hidden index names (e.g., `.migrations_working_state` → `migrations_working_state`) to comply with serverless restrictions.
  - **API Restriction Handling**: Gracefully handles unsupported cluster APIs (`/_cluster/settings`, `/_nodes`, etc.) without breaking the migration flow.
  - **Work Coordination**: Multi-worker coordination using serverless-compatible index naming and settings.
  - **Authentication Options**: Supports both API key (recommended) and basic authentication methods.
  
- **Zero-Downtime Migration with [Live Traffic Routing](docs/ClientTrafficSwinging.md)**: Tools to seamlessly switch client traffic between clusters while keeping services fully operational.

- **Migration Rollback**: Keep your source cluster synchronized during the migration, allowing you to monitor the target cluster's performance before fully committing to the switch. You can safely revert if needed.

- **User-Friendly Interface via [Migration Console](https://github.com/opensearch-project/opensearch-migrations/blob/main/docs/migration-console.md)**: Command Line Interface (CLI) that guides you through each migration step.

- **Flexible Deployment Options**:
  - **[AWS Deployment](https://aws.amazon.com/solutions/implementations/migration-assistant-for-amazon-opensearch-service/)**: Fully automated deployment to AWS.
  - **[Local Docker Deployment](/TrafficCapture/dockerSolution/README.md)**: Run the solution locally in a container for testing and development.
  - **Elasticsearch Serverless**: Direct migration to Elastic Cloud Serverless projects.
  - Contribute to add more deployment options.

## Supported Migration Paths and Platforms

### Migration Paths

| **Source Version** | **OpenSearch 1.3**                                                                     | **OpenSearch 2.19**                                                                    | **OpenSearch 3.0**                                                                     | **Elasticsearch Serverless** ⭐ _(Fork)_ |
|-------------------| -------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------- | ---------------------------------------- |
| Elasticsearch 1.x | :white_check_mark:                                                                     | :white_check_mark:                                                                     | :white_check_mark:                                                                     | :white_check_mark:                       |
| Elasticsearch 2.x | :white_check_mark:                                                                     | :white_check_mark:                                                                     | :white_check_mark:                                                                     | :white_check_mark:                       |
| Elasticsearch 5.x | :white_check_mark:                                                                     | :white_check_mark:                                                                     | :white_check_mark:                                                                     | :white_check_mark:                       |
| Elasticsearch 6.x | :white_check_mark:                                                                     | :white_check_mark:                                                                     | :white_check_mark:                                                                     | :white_check_mark:                       |
| Elasticsearch 7.x | :white_check_mark:                                                                     | :white_check_mark:                                                                     | :white_check_mark:                                                                     | :white_check_mark:                       |
| Elasticsearch 8.x |                                                                                        | :white_check_mark:                                                                     | :white_check_mark:                                                                     | :white_check_mark:                       |
| OpenSearch 1.3    | :white_check_mark:                                                                     | :white_check_mark:                                                                     | :white_check_mark:                                                                     | :white_check_mark:                       |
| OpenSearch 2.x    |                                                                                        | :white_check_mark:                                                                     | :white_check_mark:                                                                     | :white_check_mark: _(Tested with 2.13)_ |
| OpenSearch 3.x    |                                                                                        |                                                                                        | :soon: [link](https://github.com/orgs/opensearch-project/projects/229?pane=issue&itemId=117495207)                                                                     | :soon:                                   |

Note that testing is done on specific minor versions, but any minor versions within a listed major version are expected to work.

### Platforms
  - Self-managed (cloud provider hosted)
  - Self-managed (on-premises)
  - Managed cloud offerings (e.g., Amazon OpenSearch)
  - **Elasticsearch Serverless (Elastic Cloud)** ⭐ _(Fork Extension)_
    - Requires `--target-type ELASTICSEARCH_SERVERLESS` flag
    - Supports API key and basic authentication
    - Automatic settings sanitization
    - See [Elasticsearch Serverless Migration Guide](DocumentsFromSnapshotMigration/ELASTICSEARCH_SERVERLESS.md)

### Performance Limitations
A performance test was performed on 03/10/25 alongside [PR 1337](https://github.com/opensearch-project/opensearch-migrations/pull/1337)
to identify general indexing throughput with Reindex-From-Snapshot and Traffic Replay. While Reindex-From-Snapshot
includes periods of ingestion inactivity (e.g. while downloading the shard data), this was not factored into the ingestion rates.
Test used docs with an average uncompressed size 2.39 KB (source doc and index command) which corresponded to 1.54 KB of primary shard size per doc on OS 2.17 with default settings

For real-word use, throughput can be increased by vertically scaling Reindex-From-Snapshot and Traffic Replay instances or horizontally scaling
the Reindex-From-Snapshot workers that are running on AWS Fargate. Outside the exception indicated, all cases are CPU limited for throughput.
All cases were using uncompressed network traffic generated by the applications, results will vary if using client compression.

Throughput here is measured by the rate of increase in primary shard data with bulk ingestion on the target cluster alongside
the uncompressed size of the source data ingested.

Tests were ran with and without the [Type Mapping Sanitization Transformer](./transformation/transformationPlugins/jsonMessageTransformers/jsonTypeMappingsSanitizationTransformer/README.md).

#### Test Results
| Service               | vCPU | Memory (GB) | Type Mapping Sanitization | Peak Docs Ingested per minute | Primary Shard Data Ingestion Rate (MBps) | Uncompressed Source Data Ingestion Rate (MBps) |
| --------------------- | ---- | ----------- | ------------------------- | ----------------------------- | ---------------------------------------- | ---------------------------------------------- |
| Reindex-From-Snapshot | 2    | 4           | Disabled                  | 590,000                       | 15.1                                     | 23.5                                           |
| Reindex-From-Snapshot | 2    | 4           | Enabled                   | 546,000                       | 14.0                                     | 21.7                                           |
| Traffic Replay        | 8    | 48          | Disabled                  | 1,694,000 *[1]*               | 43.5  *[1]*                              | 67.5   *[1]*                                   |
| Traffic Replay        | 8    | 48          | Enabled                   | 1,645,000                     | 42.2                                     | 65.5                                           |

**[1] Network Bandwidth Limitations Observed**

**Note on Elasticsearch Serverless Performance:**
- Serverless automatically scales based on workload
- Initial performance may be slower as the platform scales up
- Performance improves as serverless adjusts to migration workload
- No manual capacity provisioning required
- Consider running multiple workers for large migrations
- See [Performance Considerations](DocumentsFromSnapshotMigration/ELASTICSEARCH_SERVERLESS.md#performance-considerations) in the Serverless Migration Guide

## Migrating to Elasticsearch Serverless

This fork adds comprehensive support for migrating to Elasticsearch Serverless (Elastic Cloud). The migration process is similar to standard migrations but includes automatic adaptations for serverless restrictions.

### Key Serverless Adaptations

| Aspect | Standard Elasticsearch | Elasticsearch Serverless | How We Handle It |
|--------|------------------------|--------------------------|------------------|
| **Work Coordination Index** | `.migrations_working_state` (hidden) | Cannot use dot-prefix indices | Automatically uses `migrations_working_state` |
| **Shard/Replica Settings** | User-configurable | Platform-managed | Automatically removed from index settings |
| **Cluster APIs** | Available | Limited/unavailable | Gracefully skipped with warnings |
| **Authentication** | Multiple options | API keys recommended | Supports both API keys and basic auth |

### Quick Start Example

```shell
./gradlew DocumentsFromSnapshotMigration:run --args="\
  --snapshot-name your-snapshot \
  --s3-repo-uri s3://your-bucket/snapshots/repo \
  --s3-region us-east-1 \
  --s3-local-dir /tmp/s3_files \
  --lucene-dir /tmp/lucene_files \
  --target-host https://your-project.es.region.aws.elastic.cloud \
  --target-api-key <your-api-key> \
  --target-type ELASTICSEARCH_SERVERLESS \
  --source-version OpenSearch_2_13"
```

### Critical Parameters for Serverless

| Parameter | Required? | Description |
|-----------|-----------|-------------|
| `--target-type ELASTICSEARCH_SERVERLESS` | **Yes** | Enables serverless mode with automatic adaptations |
| `--target-api-key` | **Recommended** | API key authentication (preferred method) |
| `--target-username` / `--target-password` | Alternative | Basic auth (use API key instead if possible) |
| `--source-version` | **Yes** | Source cluster version (e.g., `OpenSearch_2_13`) |

### Comprehensive Documentation

For detailed information on Elasticsearch Serverless migration, see:

📚 **[Elasticsearch Serverless Migration Guide](DocumentsFromSnapshotMigration/ELASTICSEARCH_SERVERLESS.md)** - Complete guide with:
  - Prerequisites and setup
  - Authentication options (API key vs basic auth)
  - Configuration examples
  - Troubleshooting common issues
  - Performance considerations
  - Best practices

📊 **[Serverless Compatibility Matrix](../docs/serverless-compatibility-matrix.md)** - Detailed feature comparison

🔧 **[Implementation Details](../docs/serverless-implementation.md)** - Technical implementation guide

### What Gets Automatically Sanitized

When migrating to Elasticsearch Serverless, the following settings are automatically removed:

**Shard/Replica Settings:**
- `index.number_of_shards`
- `index.number_of_replicas`
- `index.auto_expand_replicas`
- `index.routing.allocation.*`

**Performance Settings:**
- `index.codec`
- `index.translog.*`
- `index.merge.*`
- `index.store.*`

**Allowed Settings** (preserved during migration):
- `index.refresh_interval`
- `index.max_result_window`
- Analysis settings (analyzers, tokenizers, etc.)
- Mapping definitions

## Issue Tracking

### Fork-Specific Issues (Elasticsearch Serverless)

For issues related to **Elasticsearch Serverless support** (fork-specific features):
- Settings sanitization problems
- Work coordination index issues
- Serverless authentication problems
- Fork-specific documentation issues

Please report these in the **parent os2es project** repository, not in the upstream opensearch-migrations repository.

### Upstream Issues

For general migration issues not specific to Elasticsearch Serverless:

**Encountering a compatibility issue or missing feature?**

- [Search existing issues](https://github.com/opensearch-project/opensearch-migrations/issues) to see if it's already reported. If it is, feel free to **upvote** and **comment**.
- Can't find it? [Create a new issue](https://github.com/opensearch-project/opensearch-migrations/issues/new/choose) to let us know.

For issue prioritization and management, the migrations team uses Jira, but uses GitHub issues for community intake:

https://opensearch.atlassian.net/

### How to Determine Issue Type

| Issue Type | Report To | Examples |
|------------|-----------|----------|
| **Elasticsearch Serverless specific** | os2es project | Settings sanitization, serverless API issues, `--target-type` flag problems |
| **General migration** | Upstream repository | Snapshot reading, bulk indexing, metadata migration (non-serverless) |
| **Not sure?** | Upstream repository first | They can redirect if it's fork-specific |

## User Guide Documentation

**Upstream Documentation:**
User guide documentation for the base project is available in the [OpenSearch Migration Assistant documentation](https://docs.opensearch.org/latest/migration-assistant/).

**Fork-Specific Documentation:**
- **[Elasticsearch Serverless Migration Guide](DocumentsFromSnapshotMigration/ELASTICSEARCH_SERVERLESS.md)** - Complete guide for migrating to Elasticsearch Serverless
- **[Serverless Implementation Details](../docs/serverless-implementation.md)** - Technical implementation and architecture
- **[Serverless Compatibility Matrix](../docs/serverless-compatibility-matrix.md)** - Detailed feature compatibility reference
- **[Serverless Migration Decision Guide](../docs/serverless-migration-decision-guide.md)** - Help choosing migration approaches

## Getting Started

### Local Deployment

Refer to the [Development Guide](DEVELOPER_GUIDE.md) for more details.

### AWS Deployment

To deploy the solution on AWS, follow the steps outlined in [Migration Assistant for Amazon OpenSearch Service](https://aws.amazon.com/solutions/implementations/migration-assistant-for-amazon-opensearch-service/), specifically [deploying the solution](https://docs.aws.amazon.com/solutions/latest/migration-assistant-for-amazon-opensearch-service/deploy-the-solution.html).

### Elasticsearch Serverless Migration

⭐ **Fork Extension:** This deployment option is specific to this fork.

**Prerequisites:**
1. Source cluster (OpenSearch/Elasticsearch) with S3 snapshot
2. Elasticsearch Serverless project on Elastic Cloud
3. API key from Elastic Cloud

**Quick Start:**

```shell
# Navigate to opensearch-migrations-es directory
cd opensearch-migrations-es

# Run migration
./gradlew DocumentsFromSnapshotMigration:run --args="\
  --snapshot-name your-snapshot \
  --s3-repo-uri s3://your-bucket/snapshots/repo \
  --s3-region us-east-1 \
  --s3-local-dir /tmp/s3_files \
  --lucene-dir /tmp/lucene_files \
  --target-host https://your-project.es.region.aws.elastic.cloud \
  --target-api-key YOUR_API_KEY \
  --target-type ELASTICSEARCH_SERVERLESS \
  --source-version OpenSearch_2_13"
```

**Getting an API Key:**
1. Log into [Elastic Cloud](https://cloud.elastic.co)
2. Navigate to your Serverless project
3. Go to Management → API Keys
4. Create a new API key with appropriate permissions
5. Copy the base64-encoded key

**Important Notes:**
- The `--target-type ELASTICSEARCH_SERVERLESS` flag is **required**
- Without this flag, the migration will fail with settings errors
- API key authentication is strongly recommended over basic auth
- Settings sanitization happens automatically

**Complete Guide:** See [Elasticsearch Serverless Migration Guide](DocumentsFromSnapshotMigration/ELASTICSEARCH_SERVERLESS.md) for detailed instructions, troubleshooting, and best practices.


## Continuous Integration and Deployment
We use a combination of GitHub actions and Jenkins so that we can publish releases on a weekly basis and allow users to provide attestation for migration tooling.

Jenkins pipelines are available [here](https://migrations.ci.opensearch.org/)

## Contributing

### Contributing to This Fork

This is a fork maintained as part of the os2es project. Fork-specific changes include:
- Elasticsearch Serverless support
- Additional documentation and guides
- Testing infrastructure for serverless migrations

For fork-specific issues or enhancements, please create issues in the parent project repository.

### Contributing to Upstream

For contributions to the base OpenSearch Migration Assistant:
- Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on code of conduct and the process for submitting pull requests
- Refer to the [Development Guide](DEVELOPER_GUIDE.md) for details on building and deploying
- Visit the [upstream repository](https://github.com/opensearch-project/opensearch-migrations)

**Note:** We aim to keep this fork closely aligned with upstream and may contribute serverless support back to the main project.

## Security

See [SECURITY.md](SECURITY.md) for information about reporting security vulnerabilities.

## License

This project is licensed under the Apache-2.0 License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

### Upstream Project

This fork is based on [opensearch-project/opensearch-migrations](https://github.com/opensearch-project/opensearch-migrations):
- **OpenSearch Community** - Core migration tooling and infrastructure
- **Contributors and maintainers** - Ongoing development and maintenance
- **AWS Solutions Team** - AWS deployment automation

### Fork Enhancements

This fork extends the upstream project with Elasticsearch Serverless support:
- **os2es Project** - Serverless compatibility implementation (~120 lines of code)
- **Testing and Documentation** - Comprehensive serverless migration guides
- **Minimal Design Approach** - Easy-to-maintain fork that stays aligned with upstream

### Resources

- **Upstream Repository**: [opensearch-project/opensearch-migrations](https://github.com/opensearch-project/opensearch-migrations)
- **Upstream Documentation**: [OpenSearch Migration Assistant Docs](https://docs.opensearch.org/latest/migration-assistant/)
- **Fork Documentation**: See [DocumentsFromSnapshotMigration/ELASTICSEARCH_SERVERLESS.md](DocumentsFromSnapshotMigration/ELASTICSEARCH_SERVERLESS.md)

For more detailed information about specific components, please refer to the README files in the respective directories.
