package com.nexusversionguard.domain.port

import com.nexusversionguard.domain.model.NexusConfig

interface ConfigurationProvider {
    fun getConfig(): NexusConfig

    fun isConfigured(): Boolean
}
