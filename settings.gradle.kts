@file:Suppress("UnstableApiUsage")


dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
    }
}

rootProject.name = "TunesPipe"
include(":app")
include(":innertube")
include(":kugou")
include(":lrclib")
include(":extractor")
