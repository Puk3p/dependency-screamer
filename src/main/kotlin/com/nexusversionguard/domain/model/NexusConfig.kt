package com.nexusversionguard.domain.model

data class NexusConfig(
    val baseUrl: String = "",
    val repositories: List<String> = emptyList(),
    val username: String = "",
    val password: String = "",
    val ignoreSnapshots: Boolean = true,
    val timeoutSeconds: Int = 10,
    val groupFilter: String = "",
) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank() && repositories.isNotEmpty()
    val requiresAuth: Boolean get() = username.isNotBlank()
    val groupPrefixes: List<String>
        get() = groupFilter.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
    val hasGroupFilter: Boolean get() = groupPrefixes.isNotEmpty()
}
