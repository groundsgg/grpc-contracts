package gg.grounds.sdk

import io.nats.client.AuthHandler
import io.nats.client.Nats
import io.nats.client.Options
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

/**
 * Entry point for typed NATS event publish + subscribe.
 *
 * Reads `NATS_URL` (required) from the env. Authentication uses the
 * projected k8s ServiceAccount-Token as a NATS bearer credential —
 * the same token the gRPC services validate against the k8s JWKS
 * endpoint. Forge mounts this token at `GROUNDS_TOKEN_FILE` (default
 * `/var/run/secrets/grounds/token`). NATS broker forwards CONNECT to
 * service-nats-authz, which validates the token and signs a User-JWT
 * authorising the connection.
 *
 * Legacy fallback: if `NATS_CREDS_FILE` is set and a file exists at
 * that path, the connection authenticates with the .creds blob
 * directly. This path is kept for local NATS-without-callout dev
 * setups; in-cluster the bearer path is the standard.
 *
 * Order of precedence:
 *   1. NATS_CREDS_FILE (if set and present) — legacy / dev
 *   2. GROUNDS_TOKEN_FILE — production path (auth-callout)
 *   3. No auth — local quarkusDev / no-auth NATS
 *
 * Usage:
 *
 *     val events = GroundsEvents.connect()
 *     events.publish("match.lifecycle.ended.${matchId}", matchEnded)
 *     events.on("match.lifecycle.ended.>", MatchEnded.parser()) { event ->
 *         leaderboard.submitScore(event.winnerId, ...)
 *     }
 *     // on plugin shutdown:
 *     events.close()
 */
object GroundsEvents {

    private const val DEFAULT_TOKEN_PATH = "/var/run/secrets/grounds/token"

    fun connect(): GroundsEventsClient {
        val url =
            System.getenv("NATS_URL")
                ?: throw IllegalStateException("Missing NATS_URL env var — forge should inject this")

        val opts =
            Options.Builder()
                .server(url)
                .maxReconnects(-1)
                .reconnectWait(Duration.ofSeconds(2))
                .connectionTimeout(Duration.ofSeconds(5))

        val credsPath = System.getenv("NATS_CREDS_FILE")
        val tokenPath = System.getenv("GROUNDS_TOKEN_FILE") ?: DEFAULT_TOKEN_PATH

        if (!credsPath.isNullOrBlank() && Files.exists(Path.of(credsPath))) {
            opts.authHandler(Nats.credentials(credsPath))
        } else if (Files.exists(Path.of(tokenPath))) {
            // Re-read the file on every reconnect — the kubelet rotates
            // projected SA-Tokens well before they expire (~10min default
            // TTL) and the file content changes in-place. Connecting once
            // with a captured byte[] would lock us to the stale token.
            opts.authHandler(BearerTokenFileAuthHandler(Path.of(tokenPath)))
        }

        return GroundsEventsClient(Nats.connect(opts.build()))
    }

    /**
     * Reads the bearer token from a file on every CONNECT. NATS calls
     * `getJWT()` once per reconnect cycle, so we don't need to cache;
     * disk I/O once per ~minute (reconnect cadence) is free.
     *
     * Returns the token in the `JWT` slot of the CONNECT proto, which
     * is what NATS forwards to the auth-callout service's
     * `connect_opts.auth_token` field. The NKey/signing fields stay
     * null — auth-callout doesn't use them.
     */
    private class BearerTokenFileAuthHandler(private val tokenFile: Path) : AuthHandler {
        override fun getID(): CharArray = CharArray(0)

        override fun sign(nonce: ByteArray?): ByteArray = ByteArray(0)

        override fun getJWT(): CharArray =
            Files.readString(tokenFile).trim().toCharArray()
    }
}
