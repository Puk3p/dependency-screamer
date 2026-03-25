package com.nexusversionguard.infrastructure.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Transient
import com.nexusversionguard.domain.model.NexusConfig
import com.nexusversionguard.domain.port.ConfigurationProvider

@State(
    name = "NexusGuardSettings",
    storages = [Storage("nexus-guard-settings.xml")],
)
class NexusGuardSettings :
    PersistentStateComponent<NexusGuardSettings>,
    ConfigurationProvider {
    var baseUrl: String = ""
    var repositories: String = ""
    var username: String = ""
    var ignoreSnapshots: Boolean = true
    var timeoutSeconds: Int = 10
    var groupFilter: String = ""

    @get:Transient
    var password: String
        get() {
            val attributes = credentialAttributes()
            return PasswordSafe.instance.getPassword(attributes).orEmpty()
        }
        set(value) {
            val attributes = credentialAttributes()
            PasswordSafe.instance.set(attributes, Credentials(username, value))
        }

    private fun credentialAttributes(): CredentialAttributes {
        return CredentialAttributes(generateServiceName("DependencyScreamer", "NexusCredentials"))
    }

    override fun getState(): NexusGuardSettings = this

    override fun loadState(state: NexusGuardSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    override fun getConfig(): NexusConfig {
        return NexusConfig(
            baseUrl = baseUrl,
            repositories = repositories.split(",").map { it.trim() }.filter { it.isNotBlank() },
            username = username,
            password = password,
            ignoreSnapshots = ignoreSnapshots,
            timeoutSeconds = timeoutSeconds,
        )
    }

    fun setPasswordValue(value: String) {
        password = value
    }

    fun getPasswordValue(): String {
        return password
    }

    override fun isConfigured(): Boolean = getConfig().isConfigured

    companion object {
        fun getInstance(): NexusGuardSettings {
            return ApplicationManager.getApplication().getService(NexusGuardSettings::class.java)
        }
    }
}
