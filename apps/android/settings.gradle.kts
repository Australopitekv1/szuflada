pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "szuflada-android"

include(":core:parsing")
include(":core:protocol")

// :app wymaga Android SDK i dostępu do dl.google.com (AGP). W środowiskach
// bez tego (sandbox, szybkie testy JVM) ustaw SZUFLADA_SKIP_ANDROID=1,
// żeby budować i testować wyłącznie moduły czysto JVM-owe.
if (System.getenv("SZUFLADA_SKIP_ANDROID") == null) {
    include(":app")
}
