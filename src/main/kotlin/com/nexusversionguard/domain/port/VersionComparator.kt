package com.nexusversionguard.domain.port

import com.nexusversionguard.domain.model.VersionInfo

interface VersionComparator {
    fun isNewer(
        candidate: VersionInfo,
        current: VersionInfo,
    ): Boolean

    fun findLatest(
        versions: List<VersionInfo>,
        ignoreSnapshots: Boolean,
    ): VersionInfo?

    fun parse(versionString: String): VersionInfo
}
