pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // ⚡ Thêm repo snapshot mới của Jetpack Compose
        maven { url = uri("https://androidx.dev/snapshots/builds/11512375/artifacts/repository/") }
        maven { url = uri("https://androidx.dev/storage/compose-1.8.0-alpha01/repository/") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // ⚡ Quan trọng: giữ 2 dòng này
        maven { url = uri("https://androidx.dev/snapshots/builds/11512375/artifacts/repository/") }
        maven { url = uri("https://androidx.dev/storage/compose-1.8.0-alpha01/repository/") }
    }
}

rootProject.name = "NarukamiNews"
include(":app")
