package com.nexusversionguard.ui.settings

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class NexusGuardSettingsPanel {
    private val baseUrlField = JBTextField()
    private val repositoriesField = JBTextField()
    private val usernameField = JBTextField()
    private val passwordField = JBPasswordField()
    private val ignoreSnapshotsCheckbox = JBCheckBox("Ignore SNAPSHOT versions")
    private val timeoutSpinner = JSpinner(SpinnerNumberModel(10, 1, 120, 1))

    val rootPanel: JPanel =
        FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Nexus Base URL:"), baseUrlField)
            .addLabeledComponent(JBLabel("Repositories (comma-separated):"), repositoriesField)
            .addSeparator()
            .addLabeledComponent(JBLabel("Username:"), usernameField)
            .addLabeledComponent(JBLabel("Password:"), passwordField)
            .addSeparator()
            .addComponent(ignoreSnapshotsCheckbox)
            .addLabeledComponent(JBLabel("Timeout (seconds):"), timeoutSpinner)
            .addComponentFillVertically(JPanel(), 0)
            .panel

    var baseUrl: String
        get() = baseUrlField.text
        set(value) {
            baseUrlField.text = value
        }

    var repositories: String
        get() = repositoriesField.text
        set(value) {
            repositoriesField.text = value
        }

    var username: String
        get() = usernameField.text
        set(value) {
            usernameField.text = value
        }

    var password: String
        get() = String(passwordField.password)
        set(value) {
            passwordField.text = value
        }

    var ignoreSnapshots: Boolean
        get() = ignoreSnapshotsCheckbox.isSelected
        set(value) {
            ignoreSnapshotsCheckbox.isSelected = value
        }

    var timeoutSeconds: Int
        get() = timeoutSpinner.value as Int
        set(value) {
            timeoutSpinner.value = value
        }
}
