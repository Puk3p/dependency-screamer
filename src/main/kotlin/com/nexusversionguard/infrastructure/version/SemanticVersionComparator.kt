package com.nexusversionguard.infrastructure.version

import com.nexusversionguard.domain.model.VersionInfo
import com.nexusversionguard.domain.port.VersionComparator
import org.apache.maven.artifact.versioning.ComparableVersion

class SemanticVersionComparator : VersionComparator {
    override fun isNewer(
        candidate: VersionInfo,
        current: VersionInfo,
    ): Boolean {
        val candidateVersion = ComparableVersion(candidate.version)
        val currentVersion = ComparableVersion(current.version)
        return candidateVersion > currentVersion
    }

    override fun findLatest(
        versions: List<VersionInfo>,
        ignoreSnapshots: Boolean,
    ): VersionInfo? {
        val filtered =
            if (ignoreSnapshots) {
                versions.filter { !it.isSnapshot }
            } else {
                versions
            }

        return filtered.maxWithOrNull { a, b ->
            ComparableVersion(a.version).compareTo(ComparableVersion(b.version))
        }
    }

    override fun parse(versionString: String): VersionInfo {
        return VersionInfo(
            version = versionString,
            isSnapshot = versionString.uppercase().endsWith("-SNAPSHOT"),
        )
    }
}
