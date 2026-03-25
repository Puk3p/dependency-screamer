package com.nexusversionguard.domain.port

import com.nexusversionguard.domain.model.MavenDependency

interface DependencySource {
    fun extractDependencies(content: String): List<MavenDependency>
}
