package com.nexusversionguard.application.service

import com.nexusversionguard.domain.model.DependencyStatus
import com.nexusversionguard.domain.model.MavenCoordinates
import com.nexusversionguard.domain.model.MavenDependency
import com.nexusversionguard.domain.model.NexusConfig
import com.nexusversionguard.domain.model.VersionInfo
import com.nexusversionguard.domain.port.ArtifactRepositoryClient
import com.nexusversionguard.domain.port.ConfigurationProvider
import com.nexusversionguard.domain.port.DependencySource
import com.nexusversionguard.domain.port.VersionComparator
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.concurrent.CompletableFuture

class DependencyAnalysisServiceTest {
    private val dependencySource: DependencySource = mock()
    private val repositoryClient: ArtifactRepositoryClient = mock()
    private val versionComparator: VersionComparator = mock()
    private val configurationProvider: ConfigurationProvider = mock()

    private lateinit var service: DependencyAnalysisService

    private val configuredNexus =
        NexusConfig(
            baseUrl = "http://nexus.local",
            repositories = listOf("maven-releases"),
        )

    private val configuredWithFilter =
        NexusConfig(
            baseUrl = "http://nexus.local",
            repositories = listOf("maven-releases"),
            groupFilter = "com.endava",
        )

    @Before
    fun setUp() {
        whenever(configurationProvider.isConfigured()).thenReturn(true)
        whenever(configurationProvider.getConfig()).thenReturn(configuredNexus)

        service =
            DependencyAnalysisService(
                dependencySource = dependencySource,
                repositoryClient = repositoryClient,
                versionComparator = versionComparator,
                configurationProvider = configurationProvider,
            )
    }

    @Test
    fun `returns empty list when not configured`() {
        whenever(configurationProvider.isConfigured()).thenReturn(false)

        val results = service.analyzeDependencies("<project/>").join()

        assertThat(results).isEmpty()
    }

    @Test
    fun `marks unresolved dependency`() {
        val dep =
            MavenDependency(
                coordinates = MavenCoordinates("org.example", "lib"),
                version = "\${missing}",
                rawVersion = "\${missing}",
                lineNumber = 10,
                isPropertyBased = true,
            )
        whenever(dependencySource.extractDependencies(any())).thenReturn(listOf(dep))

        val results = service.analyzeDependencies("<project/>").join()

        assertThat(results).hasSize(1)
        assertThat(results[0].status).isEqualTo(DependencyStatus.UNRESOLVED)
    }

    @Test
    fun `marks outdated dependency`() {
        val dep =
            MavenDependency(
                coordinates = MavenCoordinates("org.example", "lib"),
                version = "1.0.0",
                rawVersion = "1.0.0",
                lineNumber = 10,
                isPropertyBased = false,
            )
        val versions = listOf(VersionInfo("1.0.0"), VersionInfo("2.0.0"))

        whenever(dependencySource.extractDependencies(any())).thenReturn(listOf(dep))
        whenever(repositoryClient.fetchAvailableVersions(any()))
            .thenReturn(CompletableFuture.completedFuture(versions))
        whenever(versionComparator.findLatest(any(), any())).thenReturn(VersionInfo("2.0.0"))
        whenever(versionComparator.parse("1.0.0")).thenReturn(VersionInfo("1.0.0"))
        whenever(versionComparator.isNewer(VersionInfo("2.0.0"), VersionInfo("1.0.0"))).thenReturn(true)

        val results = service.analyzeDependencies("<project/>").join()

        assertThat(results).hasSize(1)
        assertThat(results[0].status).isEqualTo(DependencyStatus.OUTDATED)
        assertThat(results[0].latestVersion!!.version).isEqualTo("2.0.0")
    }

    @Test
    fun `marks up-to-date dependency`() {
        val dep =
            MavenDependency(
                coordinates = MavenCoordinates("org.example", "lib"),
                version = "2.0.0",
                rawVersion = "2.0.0",
                lineNumber = 10,
                isPropertyBased = false,
            )
        val versions = listOf(VersionInfo("1.0.0"), VersionInfo("2.0.0"))

        whenever(dependencySource.extractDependencies(any())).thenReturn(listOf(dep))
        whenever(repositoryClient.fetchAvailableVersions(any()))
            .thenReturn(CompletableFuture.completedFuture(versions))
        whenever(versionComparator.findLatest(any(), any())).thenReturn(VersionInfo("2.0.0"))
        whenever(versionComparator.parse("2.0.0")).thenReturn(VersionInfo("2.0.0"))
        whenever(versionComparator.isNewer(VersionInfo("2.0.0"), VersionInfo("2.0.0"))).thenReturn(false)

        val results = service.analyzeDependencies("<project/>").join()

        assertThat(results).hasSize(1)
        assertThat(results[0].status).isEqualTo(DependencyStatus.UP_TO_DATE)
    }

    @Test
    fun `marks not-found when no versions returned`() {
        val dep =
            MavenDependency(
                coordinates = MavenCoordinates("org.example", "lib"),
                version = "1.0.0",
                rawVersion = "1.0.0",
                lineNumber = 10,
                isPropertyBased = false,
            )

        whenever(dependencySource.extractDependencies(any())).thenReturn(listOf(dep))
        whenever(repositoryClient.fetchAvailableVersions(any()))
            .thenReturn(CompletableFuture.completedFuture(emptyList()))

        val results = service.analyzeDependencies("<project/>").join()

        assertThat(results).hasSize(1)
        assertThat(results[0].status).isEqualTo(DependencyStatus.NOT_FOUND)
    }

    @Test
    fun `marks error when fetch fails`() {
        val dep =
            MavenDependency(
                coordinates = MavenCoordinates("org.example", "lib"),
                version = "1.0.0",
                rawVersion = "1.0.0",
                lineNumber = 10,
                isPropertyBased = false,
            )

        whenever(dependencySource.extractDependencies(any())).thenReturn(listOf(dep))
        whenever(repositoryClient.fetchAvailableVersions(any()))
            .thenReturn(CompletableFuture.failedFuture(RuntimeException("Connection refused")))

        val results = service.analyzeDependencies("<project/>").join()

        assertThat(results).hasSize(1)
        assertThat(results[0].status).isEqualTo(DependencyStatus.ERROR)
    }

    @Test
    fun `clearCache empties cached results`() {
        val dep =
            MavenDependency(
                coordinates = MavenCoordinates("org.example", "lib"),
                version = "1.0.0",
                rawVersion = "1.0.0",
                lineNumber = 10,
                isPropertyBased = false,
            )
        val versions = listOf(VersionInfo("2.0.0"))

        whenever(dependencySource.extractDependencies(any())).thenReturn(listOf(dep))
        whenever(repositoryClient.fetchAvailableVersions(any()))
            .thenReturn(CompletableFuture.completedFuture(versions))
        whenever(versionComparator.findLatest(any(), any())).thenReturn(VersionInfo("2.0.0"))
        whenever(versionComparator.parse("1.0.0")).thenReturn(VersionInfo("1.0.0"))
        whenever(versionComparator.isNewer(any(), any())).thenReturn(true)

        service.analyzeDependencies("<project/>").join()
        assertThat(service.getCachedResult("org.example:lib")).isNotNull

        service.clearCache()
        assertThat(service.getCachedResult("org.example:lib")).isNull()
    }

    @Test
    fun `group filter excludes non-matching dependencies`() {
        whenever(configurationProvider.getConfig()).thenReturn(configuredWithFilter)

        val endavaDep =
            MavenDependency(
                coordinates = MavenCoordinates("com.endava.xelerator", "lib"),
                version = "1.0.0",
                rawVersion = "1.0.0",
                lineNumber = 10,
                isPropertyBased = false,
            )
        val otherDep =
            MavenDependency(
                coordinates = MavenCoordinates("org.springframework", "spring-core"),
                version = "6.0.0",
                rawVersion = "6.0.0",
                lineNumber = 20,
                isPropertyBased = false,
            )

        whenever(dependencySource.extractDependencies(any())).thenReturn(listOf(endavaDep, otherDep))
        whenever(repositoryClient.fetchAvailableVersions(any()))
            .thenReturn(CompletableFuture.completedFuture(emptyList()))

        val results = service.analyzeDependencies("<project/>").join()

        assertThat(results).hasSize(1)
        assertThat(results[0].dependency.groupId).isEqualTo("com.endava.xelerator")
    }

    @Test
    fun `group filter is case insensitive`() {
        val upperCaseFilter =
            NexusConfig(
                baseUrl = "http://nexus.local",
                repositories = listOf("maven-releases"),
                groupFilter = "COM.ENDAVA",
            )
        whenever(configurationProvider.getConfig()).thenReturn(upperCaseFilter)

        val dep =
            MavenDependency(
                coordinates = MavenCoordinates("com.endava.lib", "something"),
                version = "1.0.0",
                rawVersion = "1.0.0",
                lineNumber = 10,
                isPropertyBased = false,
            )

        whenever(dependencySource.extractDependencies(any())).thenReturn(listOf(dep))
        whenever(repositoryClient.fetchAvailableVersions(any()))
            .thenReturn(CompletableFuture.completedFuture(emptyList()))

        val results = service.analyzeDependencies("<project/>").join()

        assertThat(results).hasSize(1)
    }

    @Test
    fun `empty group filter returns all dependencies`() {
        val noFilter =
            NexusConfig(
                baseUrl = "http://nexus.local",
                repositories = listOf("maven-releases"),
                groupFilter = "",
            )
        whenever(configurationProvider.getConfig()).thenReturn(noFilter)

        val dep1 =
            MavenDependency(
                coordinates = MavenCoordinates("com.endava", "a"),
                version = "1.0",
                rawVersion = "1.0",
                lineNumber = 10,
                isPropertyBased = false,
            )
        val dep2 =
            MavenDependency(
                coordinates = MavenCoordinates("org.other", "b"),
                version = "2.0",
                rawVersion = "2.0",
                lineNumber = 20,
                isPropertyBased = false,
            )

        whenever(dependencySource.extractDependencies(any())).thenReturn(listOf(dep1, dep2))
        whenever(repositoryClient.fetchAvailableVersions(any()))
            .thenReturn(CompletableFuture.completedFuture(emptyList()))

        val results = service.analyzeDependencies("<project/>").join()

        assertThat(results).hasSize(2)
    }
}
