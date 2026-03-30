plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.1.0"
    id("com.diffplug.spotless") version "6.25.0"
    id("com.github.spotbugs") version "6.0.9"
}

group = "com.dependencyscreamer"
version = "1.2.1"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.3.1")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.idea.maven")

        pluginVerifier()
        zipSigner()
        instrumentationTools()
    }

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.apache.maven:maven-artifact:3.9.6")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.assertj:assertj-core:3.25.3")
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        id = "com.dependencyscreamer"
        name = "Dependency Screamer"
        version = project.version.toString()
        description = "Checks local pom.xml dependencies against Nexus and warns when newer versions are available."

        vendor {
            name = "George Lupu"
            email = "george.lupu.dev@gmail.com"
            url = "https://github.com/Puk3p/dependency-screamer"
        }

        ideaVersion {
            sinceBuild = "243"
            untilBuild = "261.*"
        }
    }

    signing {
        certificateChainFile = providers.environmentVariable("CERTIFICATE_CHAIN").map { file(it) }
        privateKeyFile = providers.environmentVariable("PRIVATE_KEY").map { file(it) }
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("1.2.1")
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint("1.2.1")
    }
}

spotbugs {
    ignoreFailures.set(true)
    showStackTraces.set(true)
    showProgress.set(true)
    effort.set(com.github.spotbugs.snom.Effort.MAX)
    reportLevel.set(com.github.spotbugs.snom.Confidence.MEDIUM)
}

tasks {
    test {
        useJUnit()
    }

    buildSearchableOptions {
        enabled = false
    }

    withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
        reports.create("html") { required.set(true) }
        reports.create("xml") { required.set(false) }
    }
}
