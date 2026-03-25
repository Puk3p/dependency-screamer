package com.nexusversionguard.domain.model

data class MavenCoordinates(
    val groupId: String,
    val artifactId: String,
) {
    val key: String get() = "$groupId:$artifactId"

    override fun toString(): String = key
}
