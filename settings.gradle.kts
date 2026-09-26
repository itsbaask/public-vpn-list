pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://android-sdk.is.com/") } // Unity Ads
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") } // sing-box libbox
        maven { url = uri("https://maven.sagernet.org/repository/releases/") } // sing-box stable releases
    }
}

rootProject.name = "Corvus VPN"
include(":app")
include(":openvpn")
include(":tlsexternalcertprovider")
