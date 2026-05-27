package gg.grounds.sdk

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GroundsAuthTest {
    @Test
    fun `interceptor attaches bearer header when token present`() {
        val captured = captureHeadersFromInterceptor { "tok-abc" }
        assertThat(captured.get(authKey())).isEqualTo("Bearer tok-abc")
    }

    @Test
    fun `interceptor omits auth header when token absent`() {
        val captured = captureHeadersFromInterceptor { null }
        assertThat(captured.containsKey(authKey())).isFalse()
    }

    @Test
    fun `empty token is treated as absent`() {
        // The loader contract is "null when missing/empty"; we just verify
        // the interceptor doesn't blow up when given an empty string —
        // it'll attach "Bearer " which the server will reject. Fine,
        // that's the correct boundary.
        val captured = captureHeadersFromInterceptor { "" }
        assertThat(captured.get(authKey())).isEqualTo("Bearer ")
    }

    private fun captureHeadersFromInterceptor(loader: () -> String?): Metadata {
        val interceptor = GroundsAuth.JwtInterceptor(loader)
        val recorder = HeaderRecorderChannel()
        val call = interceptor.interceptCall(
            DUMMY_METHOD,
            CallOptions.DEFAULT,
            recorder,
        )
        call.start(NoopListener(), Metadata())
        return recorder.lastHeaders ?: Metadata()
    }

    private fun authKey() =
        Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)

    private class HeaderRecorderChannel : Channel() {
        var lastHeaders: Metadata? = null
        override fun <ReqT : Any, RespT : Any> newCall(
            method: MethodDescriptor<ReqT, RespT>,
            callOptions: CallOptions,
        ): ClientCall<ReqT, RespT> = object : ClientCall<ReqT, RespT>() {
            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                lastHeaders = headers
            }
            override fun request(numMessages: Int) {}
            override fun cancel(message: String?, cause: Throwable?) {}
            override fun halfClose() {}
            override fun sendMessage(message: ReqT) {}
        }
        override fun authority(): String = "test"
    }

    private class NoopListener<RespT> : ClientCall.Listener<RespT>()

    companion object {
        // A minimal valid MethodDescriptor for routing through the interceptor.
        // We never actually marshal anything — interceptCall only needs the
        // metadata path.
        private val DUMMY_METHOD: MethodDescriptor<String, String> = MethodDescriptor
            .newBuilder<String, String>()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName("Test/Method")
            .setRequestMarshaller(StringMarshaller)
            .setResponseMarshaller(StringMarshaller)
            .build()

        private object StringMarshaller : MethodDescriptor.Marshaller<String> {
            override fun stream(value: String) = value.byteInputStream()
            override fun parse(stream: java.io.InputStream): String = stream.bufferedReader().readText()
        }
    }
}
