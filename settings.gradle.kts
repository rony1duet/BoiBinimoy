try {
    val processEnvClass = Class.forName("java.lang.ProcessEnvironment")
    val varClass = Class.forName("java.lang.ProcessEnvironment\$Variable")
    val valueOfMethod = varClass.getDeclaredMethod("valueOf", String::class.java).apply { isAccessible = true }
    val varObject = valueOfMethod.invoke(null, "ANDROID_PREFS_ROOT")

    val envField = processEnvClass.getDeclaredField("theEnvironment").apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    val env = envField.get(null) as? MutableMap<Any, Any>
    val removed = env?.remove(varObject)
    println("REMOVED PREFS_ROOT: $removed")
} catch (e: Throwable) {
    println("REFLECTION EXCEPTION: $e")
}

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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Boi Binimoy"
include(":app")
