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
        // For Razorpay SDK
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "EazyDelivery"
include(":app")
