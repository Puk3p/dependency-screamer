package com.nexusversionguard.domain.port

import com.nexusversionguard.domain.model.MavenCoordinates
import com.nexusversionguard.domain.model.VersionInfo
import java.util.concurrent.CompletableFuture

interface ArtifactRepositoryClient {
    fun fetchAvailableVersions(coordinates: MavenCoordinates): CompletableFuture<List<VersionInfo>>

    fun fetchLatestVersion(coordinates: MavenCoordinates): CompletableFuture<VersionInfo?>

    fun isAvailable(): CompletableFuture<Boolean>
}
