# select-algorithm-java

Compare **IVF, HNSW and DiskANN × COS, L2 and IP** on Azure DocumentDB using precomputed document **and query** vectors. No Azure OpenAI account, API key, embedding deployment or model quota is required. This is a small-fixture functional comparison, **not a performance, recall or algorithm-selection benchmark**. Raw scores across different metrics are not comparable; a self-query need not rank first under inner product when vectors have different norms.

Standalone downstream of [the Microsoft sample](https://github.com/Azure-Samples/documentdb-samples/tree/f5a2675bbf349737234d0391e3ebee24b260e06f/ai/select-algorithm-java). Original [MIT license](LICENSE.md) and [upstream provenance](UPSTREAM.md) are preserved.

## Safety and prerequisites

- **JDK 21+ and Maven 3.9+**, Python 3 for the optional public dependency audit.
- An explicitly approved, isolated **Azure DocumentDB** database on a tier supporting all three vector indexes. Confirm current tier/region/server-version support before paying for resources. A build/mock test does **not** prove Azure compatibility.
- Either native database credentials (SCRAM-SHA-256), or an Entra identity independently registered and granted database permissions. `az login` alone does not grant DocumentDB access.
- Network/firewall access, valid TLS certificates and hostnames. Certificate validation cannot be disabled by an environment flag. Only validated public Azure DocumentDB SRV host suffixes are accepted, **before any credential is created or used**; arbitrary connection URIs and loopback hosts are not accepted by the application.
- **Writes require `ALLOW_DATABASE_WRITES=true`, an explicit database and collection prefix.** No default `Hotels` target. Reserved system databases and unsafe names are rejected.

The application creates **nine separate collections**, named `<prefix>_<algorithm>_<metric>` (for example `comparison_ivf_cos`, `comparison_diskann_ip`). It only inserts missing fixture IDs (`hotel:<HotelId>`) and creates missing indexes. It never deletes, resets, replaces or drops documents, indexes, collections or databases. Matching existing fixture documents and exact relevant index options are checked on every run, independently of collection size. Conflicts fail that case without changing the conflicting data/index. Unrelated documents are preserved and included in `totalDocuments`; their vectors may participate in searches. Use a dedicated prefix for a meaningful comparison.

Concurrent writers are not supported. A failed batch may leave a subset of newly inserted fixture documents; a rerun verifies those records and inserts only missing IDs. There is no transaction/rollback or automatic cleanup. The resource owner must review and manually clean up disposable resources after testing. This application does not provision anything.

## 1. Prepare data without a model call

Follow [data/README.md](data/README.md) to download and verify the pinned **50 × 1536** hotel fixture. Create a numeric-array query file from one existing vector for a deterministic smoke query:

```sh
python3 scripts/create_query_vector.py data/Hotels_Vector.json data/query-vector.json
```

This refuses to overwrite a query file, validates the fixture checksum and dimensions, and writes only a local query vector. It tests vector plumbing, not natural-language understanding. For a semantic query, supply a vector from the **same embedding model/space and dimension** as your documents. This repository intentionally has no optional live-LLM path; model integrations/quota are neither required nor claimed tested.

## 2. Build and test before granting access

```sh
mvn -B -ntp verify
mvn -B -ntp dependency:tree -DoutputType=json -DoutputFile=target/dependencies.json
python3 scripts/audit_dependencies.py target/dependencies.json
```

Default tests use mocks, fixture files and a bounded closed-loopback Mongo failure; no Azure resources, cloud credentials or model calls are used. The OSV audit sends public Maven package coordinates only and fails closed if the audit is unavailable or reports vulnerabilities. It covers resolved application **and test** dependencies, not the JDK, Maven plugins or an absolute security guarantee.

## 3. Configure only the approved target

The application reads **process environment variables**; it does not automatically load or execute `.env`. `.env.example` is a placeholder-only reference. Never commit credentials or shell transcripts containing them.

Required settings:

| Variable | Meaning |
| --- | --- |
| `DOCUMENTDB_HOST` | Exact SRV host, e.g. `<cluster>.global.mongocluster.cosmos.azure.com`; no scheme, port or URI options |
| `DOCUMENTDB_DATABASE` | Explicit database, letters/digits/underscore, starts with a letter, max 48 characters |
| `DOCUMENTDB_COLLECTION_PREFIX` | Explicit prefix, same name restrictions; nine suffixes are appended |
| `ALLOW_DATABASE_WRITES` | Exactly `true` |
| `DOCUMENTDB_AUTH` | Exactly `native` or `oidc` |
| `DOCUMENTDB_USERNAME`, `DOCUMENTDB_PASSWORD` | Native auth only; **unset both** for OIDC |
| `DATA_FILE_WITH_VECTORS` | Local path to the document JSON array |
| `QUERY_VECTOR_FILE` | Local path to a JSON numeric array |

Optional settings: `EMBEDDED_FIELD=DescriptionVector`, `EMBEDDING_DIMENSIONS=1536` (2–2000), `TOP_K=5` (1–100 and no larger than fixture size), `OPERATION_TIMEOUT_SECONDS=20` (1–60), `SEARCH_ATTEMPTS=4` (1–6), `SEARCH_RETRY_DELAY_MS=1000` (0–5000). Empty, malformed or out-of-range values fail; the first five variables above have **no defaults**. Input files must be valid, bounded JSON; vectors must have the configured dimension, finite numeric components and nonzero finite norms; HotelId values must be unique.

POSIX shell example (substitute the approved resource; set passwords via your secret manager):

```sh
export DOCUMENTDB_HOST='<approved-cluster>.global.mongocluster.cosmos.azure.com'
export DOCUMENTDB_DATABASE='approved_disposable_database'
export DOCUMENTDB_COLLECTION_PREFIX='native_comparison'
export ALLOW_DATABASE_WRITES=true DOCUMENTDB_AUTH=native
export DATA_FILE_WITH_VECTORS="$PWD/data/Hotels_Vector.json"
export QUERY_VECTOR_FILE="$PWD/data/query-vector.json"
# Set DOCUMENTDB_USERNAME / DOCUMENTDB_PASSWORD securely, not in committed files.
```

For OIDC, set `DOCUMENTDB_AUTH=oidc`, unset the native credential variables, and authenticate the intended Azure identity. `DefaultAzureCredential` supports Azure CLI and configured workload/managed identity. Choose the intended credential explicitly where appropriate (for example the SDK's `AZURE_TOKEN_CREDENTIALS` setting); avoid ambiguous ambient identities. The callback always requests `https://ossrdbms-aad.database.windows.net/.default`, never a server-provided audience, and passes a positive remaining token lifetime to MongoDB's driver. Expired/near-expiry tokens fail. Azure public authority is fixed. Azure Identity has no `close()` method in the pinned release; the application owns/closes its HTTP transport and credential executor as well as MongoDB on success and failure. No redirects or HTTP-level automatic retries are enabled on the credential transport.

## 4. Run

Preferred standalone process (POSIX):

```sh
mvn -B -ntp dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
java -cp "target/classes:$(cat target/classpath.txt)" com.azure.documentdb.selectalgorithm.Main
```

PowerShell: replace the last command with:

```powershell
java -cp "target/classes;$((Get-Content -Raw target/classpath.txt).Trim())" com.azure.documentdb.selectalgorithm.Main
```

`mvn -B -ntp exec:java -Pcompare` and `com.azure.documentdb.selectalgorithm.CompareAll` remain compatibility entrypoints. Prefer the standalone process for automation and process-lifetime checks.

Each of the nine cases emits one JSON line containing status, algorithm, metric, collection, independently verified fixture/total counts, inserted count, index reuse, and returned IDs/raw scores on success. Errors emit a safe stage and error category/code, **not credentials, raw exception messages or stack traces**. A failing case does not prevent later cases from being attempted. Empty, incomplete, missing-score or nonfinite-score results fail, never masquerade as score zero. Scores are displayed without sorting/ranking different metrics against one another.

Exit codes: **0 = all nine pass**, **1 = any case/connection/shutdown failure**, **2 = invalid configuration or fixture before connecting**. Mongo operations, server selection, credential acquisition and search retries are bounded. Nine sequential failures can still take several minutes at the configured timeouts; an external job timeout remains recommended. There is no unbounded retry loop.

## Explicit Azure integration gate

After independently authorizing the resources and the settings above:

```sh
RUN_AZURE_INTEGRATION=true mvn -B -ntp -Pintegration verify
```

Selecting the integration profile **without** the exact opt-in fails the test; it never silently skips/passes. It runs all nine combinations, then checks rerun idempotence and actual indexes/counts. Run it separately with native and OIDC credentials/prefixes. Default CI does not activate this profile, supply secrets, provision resources or change repository visibility. Live Azure compatibility and service-tier behavior require a recorded successful run on the exact code revision; local mock tests are not that evidence.

## Maintenance

Dependencies/plugins are pinned and the read-only GitHub Actions workflow builds, tests and audits without cloud access. Review upstream fixes manually, preserve attribution, and update [UPSTREAM.md](UPSTREAM.md) for local changes. Shared provisioning remains [upstream](https://github.com/Azure-Samples/documentdb-samples/tree/f5a2675bbf349737234d0391e3ebee24b260e06f/infra); inspect costs, firewall scope and security defaults before any deployment.

## Dated validation

See [TESTED.md](TESTED.md) for the real managed-Azure results, authentication/algorithm matrix, local checks and explicit limits.
