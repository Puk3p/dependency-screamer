plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

group = "com.dependencyscreamer"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.2")
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
    jvmToolchain(17)
}

intellijPlatform {
    pluginConfiguration {
        id = "com.dependencyscreamer"
        name = "Dependency Screamer"
        version = project.version.toString()
        description = "Checks local pom.xml dependencies against Nexus and warns when newer versions are available."

        ideaVersion {
            sinceBuild = "242"
            untilBuild = "251.*"
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks {
    test {
        useJUnit()
    }

    // Disable until extension classes are implemented
    buildSearchableOptions {
        enabled = false
    }
}
