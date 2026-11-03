plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "io.github.bednarz1990"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        rider(providers.gradleProperty("riderVersion").getOrElse("2026.2.3.1"))
        bundledPlugin("org.intellij.plugins.markdown")
        pluginVerifier()
        zipSigner()
    }
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }
tasks.withType<JavaCompile>().configureEach { options.release = 17 }
tasks.test { useJUnitPlatform() }

intellijPlatform {
    buildSearchableOptions = false
    pluginConfiguration {
        ideaVersion { sinceBuild = "262"; untilBuild = "262.*" }
    }
    pluginVerification {
        ides { current() }
    }
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
    publishing { token = providers.environmentVariable("PUBLISH_TOKEN") }
}

tasks.register<JavaExec>("integrationTest") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "io.github.bednarz1990.nuget.IntegrationProbe"
}
