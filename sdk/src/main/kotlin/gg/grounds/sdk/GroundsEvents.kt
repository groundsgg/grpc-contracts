package gg.grounds.sdk

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
        val credsPath = System.getenv("NATS_CREDS_FILE")
        val tokenPath = System.getenv("GROUNDS_TOKEN_FILE") ?: DEFAULT_TOKEN_PATH
        return GroundsEventsClient(Nats.connect(buildOptions(url, credsPath, tokenPath)))
    }

    /**
     * Builds the NATS connection options. Extracted from [connect] so the
     * auth wiring can be unit-tested without a live broker.
     *
     * The projected SA-Token is sent as the NATS **bearer** in the
     * `auth_token` CONNECT field via [Options.Builder.tokenSupplier]. This
     * MUST be tokenSupplier, NOT an `AuthHandler`: jnats serializes an
     * AuthHandler's `getJWT()` into the CONNECT `jwt` field, but the
     * auth-callout responder (service-nats-authz `AuthRequest`) reads
     * `connect_opts.auth_token` and rejects any connect that omits it.
     * tokenSupplier is invoked on every (re)connect, so a kubelet token
     * rotation (the file changes in-place) is picked up without locking us
     * to a stale token.
     */
    internal fun buildOptions(url: String, credsPath: String?, tokenPath: String): Options {
        val opts =
            Options.Builder()
                .server(url)
                .maxReconnects(-1)
                .reconnectWait(Duration.ofSeconds(2))
                .connectionTimeout(Duration.ofSeconds(5))

        if (!credsPath.isNullOrBlank() && Files.exists(Path.of(credsPath))) {
            // Legacy / local-dev path: a .creds blob (nkey + user JWT).
            opts.authHandler(Nats.credentials(credsPath))
        } else if (Files.exists(Path.of(tokenPath))) {
            val token = Path.of(tokenPath)
            opts.tokenSupplier { Files.readString(token).trim().toCharArray() }
        }

        return opts.build()
    }
}
