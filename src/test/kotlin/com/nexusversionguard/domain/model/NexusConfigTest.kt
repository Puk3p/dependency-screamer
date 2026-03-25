package com.nexusversionguard.domain.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class NexusConfigTest {
    @Test
    fun `isConfigured returns true when baseUrl and repositories are set`() {
        val config = NexusConfig(baseUrl = "http://nexus.local", repositories = listOf("releases"))
        assertThat(config.isConfigured).isTrue()
    }

    @Test
    fun `isConfigured returns false when baseUrl is blank`() {
        val config = NexusConfig(baseUrl = "", repositories = listOf("releases"))
        assertThat(config.isConfigured).isFalse()
    }

    @Test
    fun `isConfigured returns false when repositories are empty`() {
        val config = NexusConfig(baseUrl = "http://nexus.local", repositories = emptyList())
        assertThat(config.isConfigured).isFalse()
    }

    @Test
    fun `requiresAuth returns true when username is set`() {
        val config = NexusConfig(username = "user")
        assertThat(config.requiresAuth).isTrue()
    }

    @Test
    fun `requiresAuth returns false when username is blank`() {
        val config = NexusConfig(username = "")
        assertThat(config.requiresAuth).isFalse()
    }

    @Test
    fun `groupPrefixes parses comma-separated filter`() {
        val config = NexusConfig(groupFilter = "com.endava, com.myorg")
        assertThat(config.groupPrefixes).containsExactly("com.endava", "com.myorg")
    }

    @Test
    fun `groupPrefixes returns empty list for blank filter`() {
        val config = NexusConfig(groupFilter = "")
        assertThat(config.groupPrefixes).isEmpty()
    }

    @Test
    fun `groupPrefixes lowercases prefixes`() {
        val config = NexusConfig(groupFilter = "Com.Endava")
        assertThat(config.groupPrefixes).containsExactly("com.endava")
    }

    @Test
    fun `hasGroupFilter returns true when filter is set`() {
        val config = NexusConfig(groupFilter = "com.endava")
        assertThat(config.hasGroupFilter).isTrue()
    }

    @Test
    fun `hasGroupFilter returns false when filter is blank`() {
        val config = NexusConfig(groupFilter = "")
        assertThat(config.hasGroupFilter).isFalse()
    }
}
