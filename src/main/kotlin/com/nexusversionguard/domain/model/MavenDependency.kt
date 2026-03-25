package com.nexusversionguard.domain.model

data class MavenDependency(
    val coordinates: MavenCoordinates,
    val version: String,
    val rawVersion: String,
    val lineNumber: Int,
    val isPropertyBased: Boolean,
) {
    val groupId: String get() = coordinates.groupId
    val artifactId: String get() = coordinates.artifactId
    val isResolved: Boolean get() = !version.contains("\${")
}
