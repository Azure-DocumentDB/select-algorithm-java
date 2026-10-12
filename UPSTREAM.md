# Upstream provenance

- Source repository: https://github.com/Azure-Samples/documentdb-samples
- Exact source revision: [f5a2675bbf349737234d0391e3ebee24b260e06f](https://github.com/Azure-Samples/documentdb-samples/commit/f5a2675bbf349737234d0391e3ebee24b260e06f)
- Source directory: [ai/select-algorithm-java](https://github.com/Azure-Samples/documentdb-samples/tree/f5a2675bbf349737234d0391e3ebee24b260e06f/ai/select-algorithm-java)
- Extraction date: 2026-09-30
- License: original Microsoft MIT notice preserved verbatim in [LICENSE.md](LICENSE.md)
- History model: independently maintained source snapshot, not a GitHub fork or ownership transfer

## Standalone adaptations

Source is at repository root. The README is rewritten for that layout, and placeholder-only configuration/data setup replaces reliance on the parent checkout.
- Adapted source file: `.gitignore`
- Adapted source file: `README.md`

Hotel fixtures are not redistributed: [data/README.md](data/README.md) pins the original input locations and checksums. Saved output, Learn-only quickstart pages, and devcontainer automation are omitted. Shared infrastructure and root monorepo CI are not copied.

### 2026-10-12 safe comparison adaptation

- Updated Java target to 21 and pinned current MongoDB/Azure Identity/HTTP transport dependencies and Maven/test plugins; removed Azure OpenAI dependency and live model calls.
- Replaced destructive single-collection resets/index swaps with nine explicitly named insert-only collections; added deterministic fixture IDs, exact content/index conflict validation, and independent fixture counts.
- Added explicit target/write consent, native SCRAM-SHA-256 and Entra OIDC (fixed scope and actual expiry), strict TLS/destination validation, bounded operations/retries and owned transport cleanup.
- Added precomputed query-file workflow, accurate per-case/aggregate failure status, safe errors, unit/mock/CLI tests, a fail-closed opt-in Azure integration profile, and read-only build/test/public OSV audit CI.
- Kept the upstream IVF/HNSW/DiskANN index settings and COS/L2/IP search combinations. Removed cross-metric score-difference ranking implications. No benchmark, live model, or unrecorded Azure success is claimed.

## Maintenance

There is no automatic synchronization. Review upstream changes against this pinned baseline, apply only relevant fixes, retain notices, and record the imported revision and local adaptations here.
Issue/PR history is not transferred. The maintainers of the repository hosting this extraction own its downstream fixes; contribute reusable fixes upstream under its contribution policy.
