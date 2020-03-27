plugins {
    id("org.jetbrains.kotlin.multiplatform") version ("1.3.70")
    id("com.epam.drill.cross-compilation") version "0.16.0"
    distribution
    `maven-publish`
}

val ktorLibsVersion: String by extra
val coroutinesVersion: String by extra
val drillTransportLibVersion: String by extra
val drillApiVersion: String by extra
val drillAgentCoreVersion: String by extra


repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
    maven(url = "https://dl.bintray.com/kotlin/kotlinx/")
    maven(url = "https://dl.bintray.com/kotlin/ktor/")
    maven(url = "https://oss.jfrog.org/artifactory/list/oss-release-local")
}

configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("org.jetbrains.kotlinx:kotlinx-coroutines-core-native:$coroutinesVersion")).with(module("com.epam.drill.fork.coroutines:kotlinx-coroutines-core-native:$coroutinesVersion"))
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
                    implementation("com.epam.drill.agent:agent:$drillAgentCoreVersion")

                }
            }
        }
        mingwX64 {
            binaries.all { linkerOpts("-lpsapi", "-lwsock32", "-lws2_32", "-lmswsock") }
            binaries.sharedLib(libName, setOf(DEBUG, RELEASE))
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
        repositories {
            maven {

                url = uri("http://oss.jfrog.org/oss-release-local")
                credentials {
                    username =
                        if (project.hasProperty("bintrayUser"))
                            project.property("bintrayUser").toString()
                        else System.getenv("BINTRAY_USER")
                    password =
                        if (project.hasProperty("bintrayApiKey"))
                            project.property("bintrayApiKey").toString()
                        else System.getenv("BINTRAY_API_KEY")
                }
            }
        }

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
