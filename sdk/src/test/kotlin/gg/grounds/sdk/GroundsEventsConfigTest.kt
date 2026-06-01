package gg.grounds.sdk

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files

/**
 * Pure unit tests covering env-var resolution. Wire-level publish +
 * subscribe is exercised in the LeaderboardService e2e (S2), not here
 * — embedding a NATS server in CI buys nothing the e2e doesn't already
 * give us.
 */
class GroundsEventsConfigTest {
    private val originalUrl = System.getenv("NATS_URL")

    @BeforeEach
    fun clearEnv() {
        System.clearProperty("NATS_URL")
    }

    @AfterEach
    fun restoreEnv() {
        // can't actually restore process env on JVM; the test only
        // asserts behaviour when env is unset (we can't reliably set it
        // in-process). Documented limitation.
        if (originalUrl != null) {
            // no-op — we never set it in tests
        }
    }

    @Test
    fun `connect throws with a helpful message when NATS_URL is missing`() {
        // This will only be meaningful if NATS_URL is not set in the
        // test runner's env. CI is clean, local dev should also be.
        if (System.getenv("NATS_URL") != null) return
        assertThatThrownBy { GroundsEvents.connect() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("NATS_URL")
            .hasMessageContaining("forge")
    }

    @Test
    fun `SA-token bearer is sent in the auth_token CONNECT field, not jwt`() {
        // Regression guard for the B3 bearer bug: an AuthHandler.getJWT()
        // lands the token in the CONNECT `jwt` field, but service-nats-authz
        // reads connect_opts.auth_token. The token must go in auth_token.
        val tokenFile = Files.createTempFile("grounds-token", "")
        Files.writeString(tokenFile, "  sa-token-xyz\n") // padded → verifies trim
        try {
            val options =
                GroundsEvents.buildOptions("nats://localhost:4222", null, tokenFile.toString())
            val connect =
                options
                    .buildProtocolConnectOptionsString("nats://localhost:4222", true, null)
                    .toString()

            assertThat(connect).contains("\"auth_token\":\"sa-token-xyz\"")
            assertThat(connect).doesNotContain("\"jwt\"")
        } finally {
            Files.deleteIfExists(tokenFile)
        }
    }

    @Test
    fun `library exposes the expected public API surface`() {
        // Sanity check that the public API hasn't accidentally been
        // re-renamed. If this test breaks, update the API doc + caller
        // imports.
        val connectFn = GroundsEvents::class.java.getMethod("connect")
        assertThat(connectFn.returnType).isEqualTo(GroundsEventsClient::class.java)

        val publishFn = GroundsEventsClient::class.java.getMethod(
            "publish",
            String::class.java,
            com.google.protobuf.Message::class.java,
        )
        assertThat(publishFn.returnType).isEqualTo(Void.TYPE)
    }
}
