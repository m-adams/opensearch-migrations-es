# Migrating to Elasticsearch Serverless

## Overview

This guide explains how to use DocumentsFromSnapshotMigration (RFS) to migrate data to **Elasticsearch Serverless** on Elastic Cloud. Elasticsearch Serverless has important differences from standard Elasticsearch that require specific configuration and understanding.

## Quick Start

### Prerequisites

1. **Source**: OpenSearch cluster with S3 snapshot repository
2. **Target**: Elasticsearch Serverless project on Elastic Cloud
3. **Authentication**: API key for Elasticsearch Serverless

### Basic Command (with API Key - Recommended)

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

### Alternative: Basic Auth (Supported)

While API keys are recommended, basic authentication is also supported:

```shell
./gradlew DocumentsFromSnapshotMigration:run --args="\
  --snapshot-name your-snapshot \
  --s3-repo-uri s3://your-bucket/snapshots/repo \
  --s3-region us-east-1 \
  --s3-local-dir /tmp/s3_files \
  --lucene-dir /tmp/lucene_files \
  --target-host https://your-project.es.region.aws.elastic.cloud \
  --target-username <username> \
  --target-password <password> \
  --target-type ELASTICSEARCH_SERVERLESS \
  --source-version OpenSearch_2_13"
```

### Required Parameters for Serverless

| Parameter | Value | Notes |
|-----------|-------|-------|
| `--target-type` | `ELASTICSEARCH_SERVERLESS` | **Required** - Enables serverless compatibility |
| `--target-api-key` | Your API key | **Recommended** - API key authentication |
| `--target-username` / `--target-password` | Username/password | Alternative to API key (basic auth) |
| `--target-host` | Serverless endpoint | Must be full HTTPS URL |
| `--source-version` | e.g. `OpenSearch_2_13` | **Required** - Source cluster version |

**Note:** You must provide either `--target-api-key` (recommended) OR both `--target-username` and `--target-password` for authentication.

## Key Differences from Standard Elasticsearch

### 1. Work Coordination Index Name

**Standard Elasticsearch:**
```
.migrations_working_state (hidden index)
```

**Elasticsearch Serverless:**
```
migrations_working_state (regular index, no dot prefix)
```

**Why:** Serverless doesn't support hidden indices (names starting with `.`)

**Impact:** None - the tool automatically uses the correct name when `--target-type ELASTICSEARCH_SERVERLESS` is set.

### 2. Index Settings Sanitization

**Forbidden Settings (Automatically Removed):**

The following settings are **not supported** in Elasticsearch Serverless and are automatically removed:

#### Shard/Replica Settings
- `index.number_of_shards` - Managed automatically by serverless
- `index.number_of_replicas` - Managed automatically by serverless
- `index.auto_expand_replicas` - Not configurable
- `index.routing.allocation.*` - Not applicable

#### Codec and Store Settings
- `index.codec` - Managed by serverless
- `index.store.type` - Not configurable
- `index.store.fs.fs_lock` - Not applicable

#### Translog Settings
- `index.translog.durability` - Managed by serverless
- `index.translog.sync_interval` - Not configurable
- `index.translog.flush_threshold_size` - Not configurable

#### Merge Settings
- `index.merge.scheduler.max_thread_count` - Managed by serverless
- `index.merge.policy.*` - Not configurable

**Allowed Settings (Preserved):**
- `index.refresh_interval` - Can be set (within limits)
- `index.max_result_window` - Can be set (within limits)
- `index.max_terms_count` - Can be set
- `index.max_docvalue_fields_search` - Can be set

### 3. Authentication

**Standard Elasticsearch:**
- Basic auth (username/password)
- API keys
- TLS certificates
- SAML/OIDC

**Elasticsearch Serverless:**
- **API keys** (strongly recommended)
- Basic auth (username/password) - supported but not recommended
- Note: API keys provide better security and are the preferred authentication method

**Why API Keys are Recommended:**
- More secure (can be scoped to specific permissions)
- Easier to rotate without password changes
- Better audit trail
- Can be easily revoked without affecting other access

**Getting an API Key:**
1. Log into Elastic Cloud
2. Navigate to your Serverless project
3. Go to Management → API Keys
4. Create a new API key with appropriate permissions
5. Copy the base64-encoded key

### 4. API Limitations

**Not Available in Serverless:**
- Cluster settings API (`/_cluster/settings`) - Returns 410 Gone
- Node stats APIs - Limited or unavailable
- Shard allocation APIs - Not applicable
- Index close/open operations - May be limited

**Impact:** The tool automatically skips these APIs when serverless mode is detected.

## Migration Approaches

### Approach 1: Documents Only (Recommended for Serverless)

Let Elasticsearch automatically create indices as documents are migrated.

**Pros:**
- Simplest approach
- Fastest migration
- Automatic serverless-compatible settings
- No manual sanitization needed

**Command:**
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

### Approach 2: Metadata First (Advanced)

Create indices/templates first, then migrate documents. Requires using MetadataMigration tool separately.

**When to Use:**
- Need specific index configurations
- Have complex templates
- Want to validate structure before data migration

**Note:** Metadata must be sanitized for serverless compatibility. See MetadataMigration documentation for details.

## Configuration Examples

### Minimal Configuration (API Key - Recommended)

```shell
--target-host https://project.es.us-east-1.aws.elastic.cloud \
--target-api-key AbCdEf123456... \
--target-type ELASTICSEARCH_SERVERLESS \
--source-version OpenSearch_2_13
```

### Minimal Configuration (Basic Auth - Alternative)

```shell
--target-host https://project.es.us-east-1.aws.elastic.cloud \
--target-username elastic \
--target-password your_password \
--target-type ELASTICSEARCH_SERVERLESS \
--source-version OpenSearch_2_13
```

### With Index Filtering

```shell
--target-host https://project.es.us-east-1.aws.elastic.cloud \
--target-api-key AbCdEf123456... \
--target-type ELASTICSEARCH_SERVERLESS \
--source-version OpenSearch_2_13 \
--index-allowlist "logs_2024_01,logs_2024_02,metrics_2024"
```

### With Custom Bulk Settings

```shell
--target-host https://project.es.us-east-1.aws.elastic.cloud \
--target-api-key AbCdEf123456... \
--target-type ELASTICSEARCH_SERVERLESS \
--source-version OpenSearch_2_13 \
--documents-size-per-bulk-request 5242880 \
--max-connections 5
```

## Verification

### Check Work Coordination Index

```shell
curl -H "Authorization: ApiKey $ELASTICSEARCH_API_KEY" \
  "$ELASTICSEARCH_ENDPOINT/_cat/indices/migrations_working_state?v"
```

Expected: Should show `migrations_working_state` (no dot prefix)

### Verify Index Settings

```shell
curl -H "Authorization: ApiKey $ELASTICSEARCH_API_KEY" \
  "$ELASTICSEARCH_ENDPOINT/your-index/_settings?flat_settings=true"
```

Expected: Should NOT contain:
- `index.number_of_shards`
- `index.number_of_replicas`
- Any other forbidden settings

### Check Document Count

```shell
curl -H "Authorization: ApiKey $ELASTICSEARCH_API_KEY" \
  "$ELASTICSEARCH_ENDPOINT/your-index/_count"
```

## Troubleshooting

### Error: "Settings [index.number_of_shards] are not available in serverless mode"

**Cause:** `--target-type ELASTICSEARCH_SERVERLESS` was not specified

**Solution:** Add `--target-type ELASTICSEARCH_SERVERLESS` to your command

**Verification:** Check logs for:
```
INFO WorkCoordinatorFactory - Using Elasticsearch Serverless WorkCoordinator
```

### Error: "invalid_index_name_exception" for `.migrations_working_state`

**Cause:** Tool is trying to create hidden index

**Solution:** Ensure you're using the latest version with serverless support

**Expected Behavior:** Tool should create `migrations_working_state` without dot prefix

### Error: Authentication failure

**Cause:** Invalid credentials (API key or username/password)

**Solutions:**

**If using API Key (recommended):**
1. Verify API key is correctly copied (base64-encoded format)
2. Check API key has appropriate permissions
3. Ensure API key hasn't been deleted or expired
4. Test with curl:
```shell
curl -H "Authorization: ApiKey $ELASTICSEARCH_API_KEY" \
  "$ELASTICSEARCH_ENDPOINT/_cluster/health"
```

**If using Basic Auth:**
1. Verify username and password are correct
2. Ensure the user account has appropriate permissions
3. Test with curl:
```shell
curl -u "$USERNAME:$PASSWORD" \
  "$ELASTICSEARCH_ENDPOINT/_cluster/health"
```

**Recommendation:** Switch to API key authentication for better security and easier credential management.

### Error: Connection timeout

**Cause:** Network connectivity or incorrect endpoint

**Solution:**
1. Verify endpoint URL (should include `https://`)
2. Check firewall/security group rules
3. Ensure you have internet connectivity
4. Verify the Elasticsearch Serverless project is running

### Migration Appears Stuck

**Check Progress:**
```shell
curl -H "Authorization: ApiKey $ELASTICSEARCH_API_KEY" \
  "$ELASTICSEARCH_ENDPOINT/migrations_working_state/_search?size=100" | jq
```

**Look for:**
- Work items in "COMPLETED" status
- Recent `lastUpdateTime` timestamps
- Any failed work items

## Performance Considerations

### Serverless Auto-Scaling

Elasticsearch Serverless automatically scales based on workload:
- No need to pre-provision capacity
- Performance may start slower and improve as serverless scales up
- Consider running multiple workers for large migrations

### Bulk Request Sizing

**Recommended Settings:**
```shell
--documents-size-per-bulk-request 10485760  # 10 MiB (default)
--max-connections 10                        # Default
```

**For Slower Networks:**
```shell
--documents-size-per-bulk-request 5242880   # 5 MiB
--max-connections 5
```

## Best Practices

### 1. Test First

```shell
# Test with small index first
--index-allowlist "small_test_index"
```

### 2. Monitor Progress

- Check work coordination index regularly
- Monitor Elasticsearch Serverless metrics in Elastic Cloud console
- Watch for errors in RFS logs

### 3. Run Multiple Workers

For large migrations, run multiple instances in parallel:

```shell
# Worker 1
--worker-id worker-1

# Worker 2  
--worker-id worker-2
```

Workers coordinate via the `migrations_working_state` index to avoid duplicate work.

### 4. Handle Failures Gracefully

The tool tracks work completion. If a worker fails:
1. Simply restart it - it will pick up where it left off
2. Work coordination prevents duplicate document processing
3. Failed work items can be retried

### 5. Validate Results

After migration:
1. **Document counts**: Compare source vs target
2. **Query testing**: Verify queries work correctly
3. **Application testing**: Test your application against new cluster
4. **Performance testing**: Validate performance meets requirements

## Additional Resources

- [RFS Design Documentation](../RFS/docs/DESIGN.md)
- [Elasticsearch Serverless Documentation](https://www.elastic.co/guide/en/serverless/current/index.html)
- [Migration Testing Guide](../../docs/TESTING.md) (in parent repository)
- [Serverless Compatibility Matrix](../../docs/serverless-compatibility-matrix.md) (in parent repository)

## Support

For issues specific to:
- **RFS Tool**: Check existing issues in opensearch-migrations repository
- **Elasticsearch Serverless**: Contact Elastic Support
- **Migration Strategy**: See parent repository documentation

## Version History

- **v1.0.0** (October 2025): Initial serverless support
  - Work coordination index naming (`migrations_working_state`)
  - Automatic settings sanitization
  - API key authentication support

## Contributing

If you find issues with serverless migration:
1. Check if it's related to serverless-specific behavior
2. Document the issue with reproduction steps
3. Include target-type setting and relevant logs
4. Submit issues to the opensearch-migrations repository

