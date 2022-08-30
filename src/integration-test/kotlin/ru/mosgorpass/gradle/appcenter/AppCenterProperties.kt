package ru.mosgorpass.gradle.appcenter

import java.util.Properties

/**
 * AppCenter API properties used for integration tests.
 */
class AppCenterProperties {

    private val props =
        ClassLoader.getSystemResourceAsStream("integration-test.properties").use { inputStream ->
            Properties().apply {
                load(inputStream)
            }
        }

    /** AppCenter API token. */
    val apiToken: String = props.getProperty("apiToken")

    /** AppCenter owner name. */
    val ownerName: String = props.getProperty("ownerName")

    /** AppCenter app name. */
    val appName: String = props.getProperty("appName")
}
