package com.nexusversionguard.infrastructure.version

import com.nexusversionguard.domain.model.VersionInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SemanticVersionComparatorTest {
    private val comparator = SemanticVersionComparator()

    @Test
    fun `isNewer returns true when candidate is higher`() {
        val candidate = VersionInfo("2.0.0")
        val current = VersionInfo("1.0.0")

        assertThat(comparator.isNewer(candidate, current)).isTrue()
    }

    @Test
    fun `isNewer returns false when candidate is lower`() {
        val candidate = VersionInfo("1.0.0")
        val current = VersionInfo("2.0.0")

        assertThat(comparator.isNewer(candidate, current)).isFalse()
    }

    @Test
    fun `isNewer returns false when versions are equal`() {
        val candidate = VersionInfo("1.0.0")
        val current = VersionInfo("1.0.0")

        assertThat(comparator.isNewer(candidate, current)).isFalse()
    }

    @Test
    fun `isNewer handles minor and patch differences`() {
        assertThat(comparator.isNewer(VersionInfo("1.1.0"), VersionInfo("1.0.0"))).isTrue()
        assertThat(comparator.isNewer(VersionInfo("1.0.1"), VersionInfo("1.0.0"))).isTrue()
        assertThat(comparator.isNewer(VersionInfo("1.0.0"), VersionInfo("1.0.1"))).isFalse()
    }

    @Test
    fun `isNewer handles SNAPSHOT versions`() {
        val snapshot = VersionInfo("2.0.0-SNAPSHOT")
        val release = VersionInfo("1.9.0")

        assertThat(comparator.isNewer(snapshot, release)).isTrue()
    }

    @Test
    fun `findLatest returns highest non-snapshot when ignoring snapshots`() {
        val versions =
            listOf(
                VersionInfo("1.0.0"),
                VersionInfo("2.0.0-SNAPSHOT"),
                VersionInfo("1.5.0"),
            )

        val latest = comparator.findLatest(versions, ignoreSnapshots = true)

        assertThat(latest).isNotNull
        assertThat(latest!!.version).isEqualTo("1.5.0")
    }

    @Test
    fun `findLatest returns highest including snapshot when not ignoring`() {
        val versions =
            listOf(
                VersionInfo("1.0.0"),
                VersionInfo("2.0.0-SNAPSHOT"),
                VersionInfo("1.5.0"),
            )

        val latest = comparator.findLatest(versions, ignoreSnapshots = false)

        assertThat(latest).isNotNull
        assertThat(latest!!.version).isEqualTo("2.0.0-SNAPSHOT")
    }

    @Test
    fun `findLatest returns null for empty list`() {
        val latest = comparator.findLatest(emptyList(), ignoreSnapshots = true)

        assertThat(latest).isNull()
    }

    @Test
    fun `parse creates correct VersionInfo`() {
        val release = comparator.parse("1.2.3")
        assertThat(release.version).isEqualTo("1.2.3")
        assertThat(release.isSnapshot).isFalse()

        val snapshot = comparator.parse("1.2.3-SNAPSHOT")
        assertThat(snapshot.version).isEqualTo("1.2.3-SNAPSHOT")
        assertThat(snapshot.isSnapshot).isTrue()
    }
}
