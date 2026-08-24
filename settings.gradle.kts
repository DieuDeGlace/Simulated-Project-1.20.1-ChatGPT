pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        exclusiveContent {
            forRepository {
                maven("https://repo.spongepowered.org/repository/maven-public")
            }
            filter {
                includeGroupAndSubgroups("org.spongepowered")
            }
        }
        exclusiveContent {
            forRepository {
                maven("https://maven.minecraftforge.net")
            }
            filter {
                includeGroupAndSubgroups("net.minecraftforge")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "simulated-forge-1.20.1"

include("all-forge")
include("simulated:common")
include("simulated:forge")
include("aeronautics:common")
include("aeronautics:forge")
include("offroad:common")
include("offroad:forge")

include("aeronautics-bundled")
