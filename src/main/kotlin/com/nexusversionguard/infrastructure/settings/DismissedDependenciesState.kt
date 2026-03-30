package com.nexusversionguard.infrastructure.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "DismissedDependencies",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
)
class DismissedDependenciesState :
    PersistentStateComponent<DismissedDependenciesState> {
    var dismissed: MutableMap<String, String> = mutableMapOf()

    override fun getState(): DismissedDependenciesState = this

    override fun loadState(state: DismissedDependenciesState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun isDismissed(
        groupId: String,
        artifactId: String,
        latestVersion: String,
    ): Boolean {
        val key = "$groupId:$artifactId"
        return dismissed[key] == latestVersion
    }

    fun dismiss(
        groupId: String,
        artifactId: String,
        latestVersion: String,
    ) {
        dismissed["$groupId:$artifactId"] = latestVersion
    }

    fun restore(
        groupId: String,
        artifactId: String,
    ) {
        dismissed.remove("$groupId:$artifactId")
    }

    companion object {
        fun getInstance(project: Project): DismissedDependenciesState {
            return project.getService(DismissedDependenciesState::class.java)
        }
    }
}
