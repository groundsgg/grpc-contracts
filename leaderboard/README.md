# Leaderboard Module

Protocol Buffers definitions for `LeaderboardService` — persistent
player rankings per board, with season rollover and per-player rank
lookups.

First domain-service shipped under the v2.2
[Service Architecture](https://grounds.atlassian.net/wiki/spaces/GK/pages/216662019).
Plugins consume this via the SDK:

```kotlin
val channel = GroundsServices.channel("leaderboard")
val stub = LeaderboardServiceGrpc.newBlockingStub(channel)

stub.submitScore(SubmitScoreRequest.newBuilder()
  .setBoardId("duels.ranked")
  .setPlayerId(playerId)
  .setScore(25)
  .setMode(SubmitMode.SUBMIT_MODE_ACCUMULATE)
  .build())
```

Implementation: `service-leaderboard` (Quarkus + Postgres; Valkey
sorted-set hot-cache added post-MVP).
