package com.deviceinfo.auto

/**
 * Third-party libraries shown on the About screen.
 * Keep in sync with [app/build.gradle.kts] implementation dependencies.
 */
object AboutLibraries {
    val displayLines: List<String> = listOf(
        "AndroidX Core KTX 1.12.0",
        "AndroidX Activity 1.8.2",
        "AndroidX Lifecycle ViewModel / Runtime 2.7.0",
        "AndroidX AppCompat 1.6.1",
        "AndroidX Car App Library 1.4.0",
        "AndroidX RecyclerView 1.3.2",
        "AndroidX SwipeRefreshLayout 1.1.0",
    )
}
