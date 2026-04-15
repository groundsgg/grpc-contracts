# Config Contracts

This module contains the shared protobuf contracts for the config service.

## NATS Change Trigger

Some deployments also publish best-effort refresh triggers on the NATS subject:

```text
config.{app}.{env}.changed
```

These events are not the source of truth. Consumers must treat them as refresh triggers only and
reconcile through `GetSnapshotIfNewer`.

## NATS Payload

Current payload shape:

```json
{
  "app": "player",
  "env": "prod",
  "version": 42,
  "namespace": "feature-flags",
  "configKey": "new-ui",
  "timestamp": "2026-04-14T12:34:56Z"
}
```

Notes:

- `namespace` is omitted for app-wide refresh triggers.
- `configKey` is omitted for app-wide refresh triggers.
- Consumers must not derive correctness from payload contents alone.
