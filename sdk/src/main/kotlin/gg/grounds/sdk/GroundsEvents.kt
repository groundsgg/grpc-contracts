package gg.grounds.sdk

import io.nats.client.Connection
import io.nats.client.Nats
import io.nats.client.Options
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

/**
 * Entry point for typed NATS event publish + subscribe.
 *
 * Reads `NATS_URL` (required) and `NATS_CREDS_FILE` (optional) from
 * the env. Forge injects both per ProjectEnvironment based on the
 * plugin's `grounds.yaml` `events:` declarations; NATS account-side
 * permissions whitelist exactly the declared subjects, so a misuse
 * is rejected at the broker, not in client code.
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
    fun connect(): GroundsEventsClient {
        val url = System.getenv("NATS_URL")
            ?: throw IllegalStateException("Missing NATS_URL env var — forge should inject this")
        val credsPath = System.getenv("NATS_CREDS_FILE")

        val opts = Options.Builder()
            .server(url)
            .maxReconnects(-1)
            .reconnectWait(Duration.ofSeconds(2))
            .connectionTimeout(Duration.ofSeconds(5))

        if (!credsPath.isNullOrBlank() && Files.exists(Path.of(credsPath))) {
            opts.authHandler(Nats.credentials(credsPath))
        }

        return GroundsEventsClient(Nats.connect(opts.build()))
    }
}
