# Proxy-Registry Module

Protocol Buffers definitions for `ProxyRegistryService` — the v2.2
Service Architecture's typed wrapper around Velocity's backend
registry.

Backend Minecraft servers register themselves, heartbeat while alive,
and drain on shutdown. Plugin code (game-pod side) calls this typed
API instead of poking Velocity's internals.

## Methods

- `Register` — add a backend to the family rotation (idempotent)
- `Heartbeat` — keep registration fresh; missed heartbeats → GC
- `Drain` — graceful removal from rotation

## Why typed instead of direct Velocity-API

Today `plugin-platform-router` watches Agones GameServer CRDs and
pushes entries into Velocity directly. Wrapping it as a typed service
means we can swap the underlying mechanism (Agones-watch,
k8s-Service-discovery, dedicated registry DB) without touching every
game-pod.

Companion design doc:
[Service Architecture v2.2](https://grounds.atlassian.net/wiki/spaces/GK/pages/216662019)
