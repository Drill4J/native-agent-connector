import java.net.*

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.epam.drill.cross-compilation")
    id("com.github.hierynomus.license")
    distribution
    `maven-publish`
}

val scriptUrl: String by extra
val ktorLibsVersion: String by extra
val coroutinesVersion: String by extra
val serializationRuntimeVersion: String by extra
val drillTransportLibVersion: String by extra
val drillApiVersion: String by extra
val drillAgentCoreVersion: String by extra
val drillLoggerVersion: String by extra

apply(from = "$scriptUrl/git-version.gradle.kts")

repositories {
    mavenLocal()
    mavenCentral()
    apply(from = "$scriptUrl/maven-repo.gradle.kts")
    jcenter()
    maven(url = "https://dl.bintray.com/kotlin/kotlinx/")
    maven(url = "https://dl.bintray.com/kotlin/ktor/")
    maven(url = "https://oss.jfrog.org/artifactory/list/oss-release-local")
}

configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("org.jetbrains.kotlinx:kotlinx-coroutines-core-native:$coroutinesVersion")).with(module("org.jetbrains.kotlinx:kotlinx-coroutines-core-native:$coroutinesVersion-native-mt"))
    }

}

val libName = "agent_connector"
kotlin {

    targets {
        crossCompilation {
            common {
                dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:$serializationRuntimeVersion")
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-native:$coroutinesVersion")
                    implementation("com.epam.drill.transport:core:$drillTransportLibVersion")
                    implementation("com.epam.drill:common:$drillApiVersion")
                    implementation("com.epam.drill.logger:logger:$drillLoggerVersion")
                    implementation("com.epam.drill.agent:agent:$drillAgentCoreVersion")

                }
            }
        }
        mingwX64 {
            binaries.all { linkerOpts("-lpsapi", "-lwsock32", "-lws2_32", "-lmswsock") }
            binaries.sharedLib(libName, setOf(DEBUG, RELEASE))
            binaries.executable {
                baseName = "connector"
            }
        }
        linuxX64 {
            binaries.sharedLib(libName, setOf(DEBUG, RELEASE))
        }
        macosX64 {
            binaries.sharedLib(libName, setOf(DEBUG, RELEASE))
        }
    }

}

afterEvaluate {
    val availableTarget =
        kotlin.targets.filterIsInstance<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().filter {
            org.jetbrains.kotlin.konan.target.HostManager()
                .isEnabled(it.konanTarget)
        }
    distributions {
        availableTarget.forEach {
            sequenceOf("Debug", "Release").forEach { buildType ->
                val distributionName = it.name + buildType
                create(distributionName) {
                    distributionBaseName.set(distributionName)
                    contents {
                        from(tasks.getByPath("link${libName.capitalize()}${buildType}Shared${it.name.capitalize()}")) {
                            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                        }
                        from("build/install/$distributionName") {
                            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                        }
                    }
                }
            }
        }
    }
    publishing {

        publications {
            availableTarget.forEach {
                create<MavenPublication>("${it.name}DebugZip") {
                    artifactId = "$libName-${it.name}-debug"
                    artifact(tasks["${it.name}DebugDistZip"])
                }
                create<MavenPublication>("${it.name}ReleaseZip") {
                    artifactId = "$libName-${it.name}-release"
                    artifact(tasks["${it.name}ReleaseDistZip"])
                }
            }
        }
    }
}

val licenseFormatSettings by tasks.registering(com.hierynomus.gradle.license.tasks.LicenseFormat::class) {
    source = fileTree(project.projectDir).also {
        include("**/*.kt", "**/*.java", "**/*.groovy")
        exclude("**/.idea")
    }.asFileTree
    headerURI = URI("https://raw.githubusercontent.com/Drill4J/drill4j/develop/COPYRIGHT")
}

tasks["licenseFormat"].dependsOn(licenseFormatSettings)
