package gg.grounds.sdk

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
