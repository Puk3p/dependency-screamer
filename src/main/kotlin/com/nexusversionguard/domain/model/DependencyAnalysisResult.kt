package com.nexusversionguard.domain.model

data class DependencyAnalysisResult(
    val dependency: MavenDependency,
    val status: DependencyStatus,
    val latestVersion: VersionInfo? = null,
    val errorMessage: String? = null,
)

enum class DependencyStatus {
    UP_TO_DATE,
    OUTDATED,
    UNRESOLVED,
    NOT_FOUND,
    ERROR,
}
