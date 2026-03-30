package com.nexusversionguard.infrastructure.changelog

import org.apache.maven.artifact.versioning.ComparableVersion

data class ChangelogEntry(
    val version: String,
    val date: String?,
    val content: String,
)

class ChangelogParser {
    private val versionHeaderPattern = Regex("""^##\s+\[?(\d+\.\d+[\w.\-]*)]?\s*(?:-\s*(.+))?""")

    fun parse(markdown: String): List<ChangelogEntry> {
        val entries = mutableListOf<ChangelogEntry>()
        val lines = markdown.lines()

        var currentVersion: String? = null
        var currentDate: String? = null
        val currentContent = StringBuilder()

        for (line in lines) {
            val match = versionHeaderPattern.find(line)
            if (match != null) {
                if (currentVersion != null) {
                    entries.add(
                        ChangelogEntry(
                            version = currentVersion,
                            date = currentDate,
                            content = currentContent.toString().trim(),
                        ),
                    )
                }
                currentVersion = match.groupValues[1]
                currentDate = match.groupValues.getOrNull(2)?.trim()?.ifBlank { null }
                currentContent.clear()
            } else if (currentVersion != null) {
                currentContent.appendLine(line)
            }
        }

        if (currentVersion != null) {
            entries.add(
                ChangelogEntry(
                    version = currentVersion,
                    date = currentDate,
                    content = currentContent.toString().trim(),
                ),
            )
        }

        return entries
    }

    fun entriesBetween(
        entries: List<ChangelogEntry>,
        fromVersion: String,
        toVersion: String,
    ): List<ChangelogEntry> {
        val from = ComparableVersion(fromVersion)
        val to = ComparableVersion(toVersion)

        return entries.filter { entry ->
            val v = ComparableVersion(entry.version)
            v > from && v <= to
        }
    }
}
