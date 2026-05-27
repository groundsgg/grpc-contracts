# SDK Module

Plugin-/service-facing Kotlin SDK that resolves Grounds Domain-Services
by logical name. Reads `${SERVICE}_SERVICE_URL` env vars, builds gRPC
channels with a JWT bearer interceptor that attaches the projected
ServiceAccount-Token to every call.

Stubs themselves come from the per-domain proto modules (`player`,
`status`, `config`, `events`, …) — the SDK only handles channel
construction + auth so consumers stay decoupled from this concern.

```kotlin
val channel = GroundsServices.channel("player")
val stub = PlayerPresenceServiceGrpc.newBlockingStub(channel)
val resp = stub.tryPlayerLogin(...)
```

Companion design doc: [Service Architecture — Typed Domain Services as
Plugin API (v2.2)](https://grounds.atlassian.net/wiki/spaces/GK/pages/216662019)
