package com.nexusversionguard.domain.port

interface PropertyResolver {
    fun resolveProperties(content: String): Map<String, String>

    fun resolveValue(
        raw: String,
        properties: Map<String, String>,
    ): String
}
