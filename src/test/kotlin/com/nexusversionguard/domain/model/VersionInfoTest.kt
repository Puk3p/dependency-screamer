package com.nexusversionguard.domain.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class VersionInfoTest {
    @Test
    fun `detects SNAPSHOT version`() {
        val snapshot = VersionInfo("1.0.0-SNAPSHOT")
        assertThat(snapshot.isSnapshot).isTrue()
    }

    @Test
    fun `detects release version`() {
        val release = VersionInfo("1.0.0")
        assertThat(release.isSnapshot).isFalse()
    }

    @Test
    fun `detects lowercase snapshot`() {
        val snapshot = VersionInfo("1.0.0-snapshot")
        assertThat(snapshot.isSnapshot).isTrue()
    }

    @Test
    fun `toString returns version string`() {
        val info = VersionInfo("2.5.1")
        assertThat(info.toString()).isEqualTo("2.5.1")
    }
}
