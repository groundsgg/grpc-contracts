package gg.grounds.sdk

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import java.nio.file.Files
import java.nio.file.Path

/**
 * Reads the projected ServiceAccount JWT (kubelet rotates it ~1h before
 * expiry) and attaches it as `Authorization: Bearer ...` to every
 * outgoing gRPC call. Server-side, each Grounds Domain-Service
 * validates the JWT (audience, issuer, custom claims) and enforces its
 * own method-ACL.
 *
 * Path is `${GROUNDS_TOKEN_FILE}` or defaults to
 * `/var/run/secrets/grounds/token`. If the file is missing the
 * interceptor lets the call through without auth header — the server
 * is the source of truth for whether that's acceptable (typically: no).
 */
internal object GroundsAuth {
    private const val DEFAULT_TOKEN_PATH = "/var/run/secrets/grounds/token"

    private val AUTHORIZATION: Metadata.Key<String> =
        Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)

    fun interceptor(): ClientInterceptor = JwtInterceptor(tokenLoader = ::loadFromFile)

    private fun loadFromFile(): String? {
        val pathStr = System.getenv("GROUNDS_TOKEN_FILE") ?: DEFAULT_TOKEN_PATH
        val path = Path.of(pathStr)
        return try {
            if (Files.exists(path)) Files.readString(path).trim().ifEmpty { null } else null
        } catch (_: Exception) {
            null
        }
    }

    /** Visible for testing — accepts a custom token loader. */
    internal class JwtInterceptor(private val tokenLoader: () -> String?) : ClientInterceptor {
        override fun <ReqT : Any, RespT : Any> interceptCall(
            method: MethodDescriptor<ReqT, RespT>,
            callOptions: CallOptions,
            next: Channel,
        ): ClientCall<ReqT, RespT> {
            val delegate = next.newCall(method, callOptions)
            return object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(delegate) {
                override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                    tokenLoader()?.let { headers.put(AUTHORIZATION, "Bearer $it") }
                    super.start(responseListener, headers)
                }
            }
        }
    }
}
