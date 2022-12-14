# AppCenter Gradle Plugin

[![Gradle Plugins Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/io/github/angrycitizen/appcenter/io.github.angrycitizen.appcenter.gradle.plugin/maven-metadata.xml.svg?label=Gradle%20Plugins%20Portal)](https://plugins.gradle.org/plugin/io.github.angrycitizen.appcenter)

## Summary

This plugin allow you to upload android application to AppCenter. You can declare several
applications, the plugin will take care of uploading the APK and AAB on the right AppCenter
application

## Quick Start

File : `build.gradle`

```groovy
buildscript {
    repositories {
        maven {
            url "https://plugins.gradle.org/m2/"
        }
    }
    dependencies {
        classpath "io.github.angrycitizen:appcenter-plugin:1.1.0"
    }
}

```

File : `app/build.gradle`

```groovy

apply plugin: "io.github.angrycitizen.appcenter"

android {
    // ...
    flavorDimensions "environment"
    productFlavors {
        alpha {
            dimension "environment"
            applicationIdSuffix ".alpha"
            versionNameSuffix "-alpha"
        }
        beta {
            dimension "environment"
            applicationIdSuffix ".beta"
            versionNameSuffix "-beta"
        }
        prod {
            dimension "environment"
        }
    }
    
    buildTypes {
        release {
            signingConfig android.signingConfigs.release
        }
    }
}

appcenter {
    apiToken = "XXXXXXXX"                       // Api Token from AppCenter user profile
    ownerName = "ACME"                          // Owner Name from AppCenter Application (see following note)
    distributionGroups = ["Beta"]               // Name of the AppCenter Distribution Group
    releaseNotes = file("../changelog.md")      // Can be a file or text
    notifyTesters = true                        // Send mail to testers
    apps {                                      // Here we manage 3 AppCenter applications : alpha, beta and prod
        alpha {                                 // When dimension is provided, this name match the productFlavor name
            dimension = "environment"           // This dimension match the flavor dimension
            appName = "GradleSample-Alpha"      // The AppCenter application name
        }
        beta {
            dimension = "environment"
            appName = "GradleSample-Beta"
        }
        prodRelease {                           // When no dimension is provided, this name match the full variant name
            appName = "GradleSample"            // Application Name from AppCenter (see following note)
        }
    }
}

```

**Note** : `ownerName` and `appName` can be found from AppCenter application
url (`https://appcenter.ms/users/{ownerName}/apps/{appName}`)

The plugin will generate severals tasks for each variant :

- appCenterUploadApkAlphaRelease
- appCenterUploadAabAlphaRelease
- appCenterUploadMappingAlphaRelease # Only when `uploadMappingFiles` is set to true
- appCenterUploadSymbolsAlphaRelease # Only when `symbols` are provided in configuration
- appCenterUploadAlphaRelease

To upload an apk, just run tasks assemble and appCenterUploadApk

`./gradlew assembleAlphaRelease appCenterUploadApkAlphaRelease`

To upload an aab, just run tasks bundle and appCenterUploadAab

`./gradlew bundleAlphaRelease appCenterUploadAabAlphaRelease`

## Override properties

Each `apps` nodes inherit properties from `appcenter` global properties. You can override those
properties by defining properties on the target application node like in the following sample
with `alpha` node

```groovy
appcenter {
    apiToken = "XXXXXXXX"
    ownerName = "ACME"
    distributionGroups = ["Beta"]
    releaseNotes = file("../changelog.md")
    notifyTesters = false
    symbols = ["symbols.zip"]
    apps {      
        alpha {
            dimension = "environment"
            apiToken = "YYYYYYYY"
            ownerName = "AnotherOwner"
            distributionGroups = ["Alpha"]
            releaseNotes = "No Changes"
            appName = "GradleSample-Alpha"
            notifyTesters = true
            uploadMappingFiles = true
            symbols = ["symbols.zip"]
        }
        prodRelease {           
            appName = "GradleSample"
        }
    }
}
```

## Use Environment Variables (for CI use)

- `APPCENTER_API_TOKEN` : AppCenter API token
- `APPCENTER_OWNER_NAME` : Owner name
- `APPCENTER_DISTRIBUTION_GROUPS` : Comma separated list of distribution groups
- `APPCENTER_RELEASE_NOTES` : Release notes in Markdown format
- `APPCENTER_NOTIFY_TESTERS` : Notify testers
- `APPCENTER_SYMBOLS` : Comma separated list of symbols file to upload

## Timeouts

By default, plugin set timeouts to 60 seconds. You can override them with the following properties :

- http.timeout.connect
- http.timeout.read
- http.timeout.write

You can define those properties in your local `gradle.properties` or
global `~/.gradle/gradle.properties`
