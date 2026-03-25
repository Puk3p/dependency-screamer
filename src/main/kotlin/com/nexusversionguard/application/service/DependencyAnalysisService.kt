package com.nexusversionguard.application.service

import com.nexusversionguard.domain.model.DependencyAnalysisResult
import com.nexusversionguard.domain.model.DependencyStatus
import com.nexusversionguard.domain.model.VersionInfo
import com.nexusversionguard.domain.port.ArtifactRepositoryClient
import com.nexusversionguard.domain.port.ConfigurationProvider
import com.nexusversionguard.domain.port.DependencySource
import com.nexusversionguard.domain.port.VersionComparator
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class DependencyAnalysisService(
    private val dependencySource: DependencySource,
    private val repositoryClient: ArtifactRepositoryClient,
    private val versionComparator: VersionComparator,
    private val configurationProvider: ConfigurationProvider,
) {
    private val cache = ConcurrentHashMap<String, DependencyAnalysisResult>()

    fun analyzeDependencies(pomContent: String): CompletableFuture<List<DependencyAnalysisResult>> {
        if (!configurationProvider.isConfigured()) {
            return CompletableFuture.completedFuture(emptyList())
        }

        val dependencies = dependencySource.extractDependencies(pomContent)
        val config = configurationProvider.getConfig()

        val futures =
            dependencies.map { dependency ->
                if (!dependency.isResolved) {
                    CompletableFuture.completedFuture(
                        DependencyAnalysisResult(
                            dependency = dependency,
                            status = DependencyStatus.UNRESOLVED,
                            errorMessage = "Property could not be resolved: ${dependency.rawVersion}",
                        ),
                    )
                } else {
                    val cached = cache[dependency.coordinates.key]
                    if (cached != null && cached.dependency.version == dependency.version) {
                        CompletableFuture.completedFuture(cached)
                    } else {
                        analyzeRemote(dependency, config.ignoreSnapshots)
                    }
                }
            }

        return CompletableFuture.allOf(*futures.toTypedArray()).thenApply {
            futures.map { it.join() }
        }
    }

    fun clearCache() {
        cache.clear()
    }

    fun getCachedResult(coordinatesKey: String): DependencyAnalysisResult? {
        return cache[coordinatesKey]
    }

    private fun analyzeRemote(
        dependency: com.nexusversionguard.domain.model.MavenDependency,
        ignoreSnapshots: Boolean,
    ): CompletableFuture<DependencyAnalysisResult> {
        return repositoryClient.fetchAvailableVersions(dependency.coordinates)
            .thenApply { versions ->
                buildResult(dependency, versions, ignoreSnapshots)
            }
            .exceptionally { throwable ->
                DependencyAnalysisResult(
                    dependency = dependency,
                    status = DependencyStatus.ERROR,
                    errorMessage = "Failed to fetch versions: ${throwable.message}",
                )
            }
            .thenApply { result ->
                cache[dependency.coordinates.key] = result
                result
            }
    }

    private fun buildResult(
        dependency: com.nexusversionguard.domain.model.MavenDependency,
        versions: List<VersionInfo>,
        ignoreSnapshots: Boolean,
    ): DependencyAnalysisResult {
        if (versions.isEmpty()) {
            return DependencyAnalysisResult(
                dependency = dependency,
                status = DependencyStatus.NOT_FOUND,
                errorMessage = "Artifact not found in Nexus",
            )
        }

        val latest =
            versionComparator.findLatest(versions, ignoreSnapshots)
                ?: return DependencyAnalysisResult(
                    dependency = dependency,
                    status = DependencyStatus.NOT_FOUND,
                    errorMessage = "No suitable version found",
                )

        val currentVersion = versionComparator.parse(dependency.version)

        return if (versionComparator.isNewer(latest, currentVersion)) {
            DependencyAnalysisResult(
                dependency = dependency,
                status = DependencyStatus.OUTDATED,
                latestVersion = latest,
            )
        } else {
            DependencyAnalysisResult(
                dependency = dependency,
                status = DependencyStatus.UP_TO_DATE,
                latestVersion = latest,
            )
        }
    }
}
