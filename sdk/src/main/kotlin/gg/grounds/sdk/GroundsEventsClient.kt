package gg.grounds.sdk

import com.google.protobuf.Message
import com.google.protobuf.Parser
import io.nats.client.Connection
import io.nats.client.Dispatcher
import io.nats.client.Subscription
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Thin wrapper around a single NATS [Connection] that publishes and
 * subscribes typed Protobuf messages. Subjects are explicit per call
 * (no auto-derivation from proto package) so each event-domain can
 * use its own pattern — match.lifecycle.ended.{matchId},
 * friends.status.{recipientPlayerId}, config.{app}.{env}.changed, ...
 *
 * Closing the client closes the underlying connection.
 */
class GroundsEventsClient internal constructor(
    private val connection: Connection,
) : AutoCloseable {

    private val dispatchers = ConcurrentHashMap<String, Dispatcher>()

    /** Publish a typed protobuf message. Subject is explicit per call. */
    fun publish(subject: String, message: Message) {
        connection.publish(subject, message.toByteArray())
    }

    /**
     * Subscribe to [subject] (NATS-wildcards `*` / `>` allowed), parse
     * each payload with [parser] and invoke [handler] on a NATS
     * dispatcher worker thread.
     *
     * Inside Minecraft plugins, wrap the [handler] body with
     * `Bukkit.getScheduler().runTask(...)` if you need to touch
     * tick-thread-only state. The SDK deliberately doesn't do this
     * automatically — it stays Minecraft-runtime-agnostic.
     *
     * Returns the [Subscription]; cancel via `subscription.drain(...)`
     * or `subscription.unsubscribe()` if no longer needed.
     *
     * Parse-failures are dropped silently and the connection keeps
     * running — a malformed payload from another publisher must not
     * tear down the subscriber.
     */
    fun <T : Message> on(
        subject: String,
        parser: Parser<T>,
        handler: (T) -> Unit,
    ): Subscription {
        val dispatcher = dispatchers.computeIfAbsent(subject) {
            connection.createDispatcher { /* per-message handler set per-subscription below */ }
        }
        return dispatcher.subscribe(subject) { msg ->
            val parsed = try {
                parser.parseFrom(msg.data)
            } catch (_: Exception) {
                return@subscribe
            }
            try {
                handler(parsed)
            } catch (_: Exception) {
                // handler exception — drop, don't kill the dispatcher
            }
        }
    }

    /** Underlying connection status — useful for liveness probes. */
    fun isConnected(): Boolean = connection.status == Connection.Status.CONNECTED

    /**
     * Drain in-flight messages then close the connection. Blocks up to
     * [timeout] waiting for graceful drain.
     */
    fun close(timeout: Duration) {
        try {
            connection.drain(timeout)
        } catch (_: Exception) {
            // fall through to close
        }
        connection.close()
    }

    override fun close() = close(Duration.ofSeconds(5))
}
