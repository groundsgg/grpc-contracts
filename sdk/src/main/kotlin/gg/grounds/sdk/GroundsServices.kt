package gg.grounds.sdk

import io.grpc.ManagedChannel

/**
 * Plugin-facing facade. Resolves a typed gRPC channel for a Grounds
 * domain-service by name; the actual stub-construction is up to the
 * caller (so this stays decoupled from the per-service proto modules).
 *
 * Usage:
 *
 *     val channel = GroundsServices.channel("player")
 *     val stub = PlayerPresenceServiceGrpc.newBlockingStub(channel)
 *
 * Resolution: looks up `${SERVICE_NAME_UPPER}_SERVICE_URL` in the env
 * (e.g. `PLAYER_SERVICE_URL`). Forge injects these per ProjectEnvironment
 * based on the plugin's grounds.yaml `services:` declarations.
 *
 * Auth: each call carries a Bearer-JWT read from the projected
 * ServiceAccount-Token at `${GROUNDS_TOKEN_FILE}` (default
 * `/var/run/secrets/grounds/token`). The token is re-read per call
 * so kubelet's rotation is picked up immediately.
 */
object GroundsServices {
    fun channel(service: String): ManagedChannel = GroundsChannels.channel(service)

    /** Close all cached channels. Call on plugin shutdown. */
    fun shutdown() = GroundsChannels.shutdownAll()
}
