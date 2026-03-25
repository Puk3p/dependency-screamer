package com.nexusversionguard.domain.port

import com.nexusversionguard.domain.model.DependencyAnalysisResult

interface NotificationPresenter {
    fun presentResults(results: List<DependencyAnalysisResult>)

    fun presentError(message: String)

    fun clear()
}
