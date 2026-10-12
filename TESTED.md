# Validation record

## Real managed Azure validation — 2026-10-12

Tested on one disposable Azure DocumentDB M30 in West US 2, one shard, MongoDB 8.0 compatibility, 128 GiB storage and HA disabled. Native SCRAM-SHA-256 and Microsoft Entra OIDC were exercised separately with verified TLS and a client-IP firewall. No OpenAI/model calls were made.

Fixture: 50 public pinned records with 1,536-dimensional vectors; the query is the first stored vector. This is a numerical vector/database contract test, not semantic relevance, performance, production-readiness or a tier/version support matrix. No real credentials or infrastructure identifiers are published.

IVF/HNSW/DiskANN × COS/L2/IP passed under native and OIDC: **18/18 cases**. Each created/verified its actual index, returned five results, and reran without duplicate insertion. Standalone Java processes terminated successfully. An independent Python calculation of COS/L2/IP ranks matched all 18 actual result sets; scores were not compared across metrics.

An unrelated sentinel was retained in all 18 collections (51 documents each). A deliberate user edit to a fixture record was preserved: its case failed at preparation, the other eight cases still passed, all nine were attempted and the process exited 1. This is conflict refusal, not automatic merging of user edits.

Wrong password, disabled write opt-in, unsafe database name and foreign host each failed with a nonzero process exit and no credential disclosure. Local suite25/25 passed, OSV audit40resolved packages reported zero findings. Tools: Temurin21.0.12.1 and Maven3.9.9. No model dependency is present.

Tested implementation: `f9da82f8c49c72fc86be27b1348cb3ae4749b83c`.

## Scope and cleanup

These tests used only separately approved synthetic namespaces on a disposable cluster. The sample never automatically deletes databases, collections or indexes. Resource cleanup is an explicit environment-owner responsibility. Publication is conditioned on verifying that the test resource group is gone, hosted CI passes, and an anonymous clean checkout works. MIT licensing and pinned upstream data provenance are retained.
