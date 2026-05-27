package gg.grounds.sdk

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class GroundsChannelsTest {
    @Test
    fun `envKeyFor uppercases and replaces dashes`() {
        assertThat(GroundsChannels.envKeyFor("player")).isEqualTo("PLAYER_SERVICE_URL")
        assertThat(GroundsChannels.envKeyFor("anti-cheat")).isEqualTo("ANTI_CHEAT_SERVICE_URL")
    }

    @Test
    fun `resolveTarget reads env var`() {
        val env = mapOf("PLAYER_SERVICE_URL" to "service-player.ns.svc.cluster.local:9090")
        assertThat(GroundsChannels.resolveTarget("player", env::get))
            .isEqualTo("service-player.ns.svc.cluster.local:9090")
    }

    @Test
    fun `resolveTarget throws with a helpful message when env missing`() {
        assertThatThrownBy { GroundsChannels.resolveTarget("leaderboard") { null } }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("LEADERBOARD_SERVICE_URL")
            .hasMessageContaining("grounds.yaml")
    }
}
