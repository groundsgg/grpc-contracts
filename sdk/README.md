# SDK Module

Plugin-/service-facing Kotlin SDK for talking to Grounds Domain-Services
(gRPC) and the typed event bus (NATS). All wiring (URLs, credentials)
is read from env vars set by forge per ProjectEnvironment — code stays
config-free.

Companion design doc: [Service Architecture — Typed Domain Services as
Plugin API (v2.2)](https://grounds.atlassian.net/wiki/spaces/GK/pages/216662019)

## gRPC Services

```kotlin
val channel = GroundsServices.channel("player")
val stub = PlayerPresenceServiceGrpc.newBlockingStub(channel)
val resp = stub.tryPlayerLogin(...)
```

Resolution: `${SERVICE_UPPER}_SERVICE_URL` (e.g. `PLAYER_SERVICE_URL`).
Auth: projected ServiceAccount JWT from `${GROUNDS_TOKEN_FILE}`
(default `/var/run/secrets/grounds/token`) attached as
`Authorization: Bearer ...` per call. Token is re-read on each call so
kubelet rotation is picked up live.

Stub construction stays on the consumer — depend on the relevant
`library-grpc-contracts-<domain>` artefact for the proto stubs.

## NATS Events

```kotlin
val events = GroundsEvents.connect()

// publish a typed proto message — subject is explicit per call
events.publish("match.lifecycle.ended.$matchId", matchEndedMsg)

// subscribe with a wildcard, parsed callback
events.on("match.lifecycle.ended.>", MatchEnded.parser()) { event ->
    leaderboard.submitScore(event.winnerId, "duels", event.scoreDelta)
}

// on plugin shutdown
events.close()
```

Env-vars: `NATS_URL` (required), `NATS_CREDS_FILE` (optional
`.creds`-file path for NATS-account-based auth). NATS server-side
permissions whitelist the subjects this plugin's account is allowed to
publish/subscribe — a misuse is rejected at the broker.

The payload format is **Protobuf bytes** (not JSON). Subscribers
deserialise with the proto's static `parser()`. Subject naming is
explicit per call: each event proto's doc-comment in
`events/src/main/proto/` declares its canonical subject pattern.

Subscription callbacks run on a NATS dispatcher worker thread. Inside
Minecraft plugins, wrap with `Bukkit.getScheduler().runTask(plugin) { … }`
if the handler touches tick-thread-only state.
