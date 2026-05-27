package gg.grounds.sdk

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

internal object GroundsChannels {
    private val cache = ConcurrentHashMap<String, ManagedChannel>()

    fun channel(service: String): ManagedChannel = cache.computeIfAbsent(service) { build(it) }

    fun shutdownAll() {
        cache.values.forEach { ch ->
            ch.shutdown()
            if (!ch.awaitTermination(5, TimeUnit.SECONDS)) ch.shutdownNow()
        }
        cache.clear()
    }

    private fun build(service: String): ManagedChannel {
        val target = resolveTarget(service)
        return ManagedChannelBuilder
            .forTarget(target)
            .usePlaintext()
            .intercept(GroundsAuth.interceptor())
            .build()
    }

    /**
     * Map a logical service name ("player") to the gRPC target string by
     * reading the corresponding env var (`PLAYER_SERVICE_URL`).
     *
     * Visible for testing.
     */
    internal fun resolveTarget(
        service: String,
        env: (String) -> String? = System::getenv,
    ): String {
        val key = envKeyFor(service)
        return env(key)
            ?: throw IllegalStateException(
                "Missing env var $key — forge should inject this based on grounds.yaml services.$service"
            )
    }

    internal fun envKeyFor(service: String): String =
        "${service.uppercase().replace('-', '_')}_SERVICE_URL"
}
