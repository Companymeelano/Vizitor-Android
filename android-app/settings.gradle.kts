// Vizitor Direct — standalone Android app (direct SQL Server connection, port 1433)
// ساخته‌شده برای هماهنگی کامل با نصب‌کنندهٔ ویندوز Vizitor-Setup-1.0.0.exe
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

rootProject.name = "VizitorDirect"
include(":vizitor-direct")
