plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.epam.drill.cross-compilation")
    distribution
    `maven-publish`
}

val scriptUrl: String by extra
val ktorLibsVersion: String by extra
val coroutinesVersion: String by extra
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
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-native:$coroutinesVersion")
                    implementation("com.epam.drill.transport:core:$drillTransportLibVersion")
                    implementation("com.epam.drill:common:$drillApiVersion")
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
            val debugName = it.name + "Debug"
            create(debugName) {
                distributionBaseName.set(debugName)
                contents {
                    from(tasks.getByPath("link${libName.capitalize()}DebugShared${it.name.capitalize()}"))
                    from("build/install/$debugName")
                }
            }
            val releaseName = it.name + "Release"
            create(releaseName) {
                distributionBaseName.set(releaseName)
                contents {
                    from(tasks.getByPath("link${libName.capitalize()}ReleaseShared${it.name.capitalize()}"))
                    from("build/install/$releaseName")
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
