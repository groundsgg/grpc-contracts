# Match Module

Protocol Buffers definitions for `MatchService` — the central
matchmaker. Players queue per (project, mode, region), the service
forms MMR-based matches, allocates an Agones GameServer in the
target cluster and routes the players there via Velocity.

The same messages travel two transports:

- **NATS request-reply** at the vCluster edge — subjects
  `match.v1.<project>.<region>.{enqueue,cancel,ticket.get}`. This is
  how the Velocity proxy talks to the matchmaker.
- **gRPC** for central service-to-service calls (portal, forge).

## Project and region are subject-derived

`EnqueueRequest` deliberately carries **no project and no region**
field. Both come from the NATS subject: forge stamps them as pinned
literals into the proxy's broker grant, so a client cannot queue
into another project or region even if it tries. The only
location-ish field is `location` — a QoS hint that steers which
cluster inside the region hosts the match. It is never a queue
dimension and never a security boundary.

## Consuming it

```kotlin
val channel = GroundsServices.channel("match")
val stub = MatchServiceGrpc.newBlockingStub(channel)

val reply = stub.getRating(GetRatingRequest.newBuilder()
  .setPlayerId(playerId)
  .setModeId("mobrush")
  .build())

// Weng-Lin (OpenSkill): conservative display rating is mu - 3*sigma
println("rating: ${reply.display}")
```

Ratings are global and player-scoped — there is no per-region
ladder. Region is a queue and allocation dimension only.
