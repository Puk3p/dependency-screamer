package com.nexusversionguard.domain.model

data class VersionInfo(
    val version: String,
    val isSnapshot: Boolean = version.uppercase().endsWith("-SNAPSHOT"),
) {
    override fun toString(): String = version
}
