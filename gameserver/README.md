# Gameserver Module

Protocol Buffers definitions for `GameserverService` — the v2.2
Service Architecture's typed wrapper around the Agones GameServer
lifecycle.

Why a typed wrapper: shields plugin code from Agones SDK version drift.
Plugins call `gameserver.ready()` instead of `agones.SDK.Ready()`; the
service implementation handles the underlying Agones API.

## Methods

- `Ready` — signal the GameServer is Ready (`Scheduled` → `Ready`)
- `Shutdown` — initiate graceful shutdown
- `SetLabel` — set/update a label (prefix `gg.grounds/` enforced)
- `Allocate` — claim a Ready GameServer by label-selector (matchmaking)

## Identity

Every call carries the pod's projected ServiceAccount-JWT
(audience `grounds-services`). The service derives the calling
GameServer's name from pod identity — no need for callers to thread a
GameServer-name through every request.

Companion design doc:
[Service Architecture v2.2](https://grounds.atlassian.net/wiki/spaces/GK/pages/216662019)
