# events

NATS-Subject-Konstanten + Payload-Schemas für Cross-Service-Events
in der Grounds-Platform.

Schwester-Modul zu `player` und `status` (gRPC). Hier liegen die
Schemas für **asynchrone** Events, die über NATS pub/sub fliessen —
also alle Fälle wo Publisher und Subscriber sich nicht kennen
müssen, und 0..N Receiver mit dem gleichen Subject leben.

## Was hier liegt

```
events/src/main/proto/
├── config_events.proto    # config.{app}.{env}.changed
├── player_events.proto    # presence.{join,leave,switch}
├── friends_events.proto   # friends.status.{playerId}
├── match_events.proto     # match.{lifecycle,started,ended}.{matchId}
└── README.md (this)
```

## Subject-Konventionen

```
<domain>.<entity>.<action>[.<scope>]
```

Beispiele:
- `config.service-player.prod.changed` — domain=config, entity=service-player, action=changed, scope=prod
- `presence.player.joined.<playerId>` — domain=presence, entity=player, action=joined, scope=playerId
- `friends.status.<playerId>` — domain=friends, entity=status, scope=playerId (no action — Subject *is* the change-feed)
- `match.lifecycle.started.<matchId>` — domain=match, entity=lifecycle, action=started, scope=matchId

**Wildcard-Subscribe**: `match.lifecycle.>` für alle Match-Lifecycle-Events.

## Wie konsumieren

Per Gradle in irgendeinem Service:

```kotlin
// build.gradle.kts
dependencies {
    protobuf("gg.grounds:library-grpc-contracts-events:0.2.0")
    // … oder lokal: implementation(project(":events"))
}
```

Codegen (über `protoc-gen-grpc-kotlin` o.ä.) erzeugt Kotlin-Klassen.
Subject-Patterns leben aktuell als Doc-Comments im Proto — Phase 2
wird ein `Subjects.kt`-Object generieren das `subject(...)`-Funktionen
mit typed parameters bietet.

## Verhältnis zu gRPC

- **gRPC unary** (`status/`, `player/`): synchrone request/response,
  ein Service antwortet
- **gRPC server-streaming** (`status/SubscribeStatus`): 1-Service-zu-N-
  Subscriber Push mit subscriber-spezifischem Filter
- **NATS pub/sub** (this): N-zu-M-Broadcast, Publisher und Subscriber
  kennen sich nicht, JetStream-Replay verfügbar

Faustregel: gRPC = "frag was nach", NATS = "sag Bescheid".
NATS ersetzt gRPC nicht und umgekehrt.

Konzept-Doc:
[Cross-Proxy Sync & Multi-Cluster](https://grounds.atlassian.net/wiki/spaces/GK/pages/8421381)
+ [Platform-Test-Environment](https://grounds.atlassian.net/wiki/spaces/GK/pages/209715203)
