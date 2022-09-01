package ru.mosgorpass.gradle.appcenter

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApkVariantOutput
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.api.ApkVariantImpl
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import ru.mosgorpass.gradle.appcenter.extensions.AppCenterExtension
import ru.mosgorpass.gradle.appcenter.tasks.UploadAppCenterAppPackageTask
import ru.mosgorpass.gradle.appcenter.tasks.UploadAppCenterMappingTask
import ru.mosgorpass.gradle.appcenter.tasks.UploadAppCenterSymbolsTask
import java.io.File

class AppCenterPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        with(project) {
            extensions.create(APP_CENTER_EXTENSION_NAME, AppCenterExtension::class.java, this)

            afterEvaluate { p ->
                val androidExtension = extensions.getByName("android") as AppExtension
                val appCenterExtension =
                    extensions.getByName(APP_CENTER_EXTENSION_NAME) as AppCenterExtension
                androidExtension.applicationVariants.whenObjectAdded { variant ->
                    handleVariant(variant, appCenterExtension, p)
                }

                androidExtension.applicationVariants.forEach { variant ->
                    handleVariant(variant, appCenterExtension, p)
                }
            }
        }
    }

    private fun handleVariant(
        variant: ApplicationVariant,
        appCenterExtension: AppCenterExtension,
        project: Project
    ) {
        val apkVariant = variant as? ApkVariantImpl ?: return
        val appCenterApp = apkVariant.productFlavors.mapNotNull {
            appCenterExtension.findByFlavor(
                it.name,
                it.dimension
            )
        }.firstOrNull() ?: appCenterExtension.findByBuildVariant(apkVariant.name)

        appCenterApp?.let {
            val assembleTask = apkVariant.assembleProvider.get()
            val bundleTask = apkVariant.variantData.taskContainer.bundleTask!!.get()

            apkVariant.outputs.all { output ->
                if (output is ApkVariantOutput) {
                    val filterIdentifiersCapitalized =
                        output.filters.joinToString("") { it.identifier.capitalize() }
                    val taskSuffix = "${apkVariant.name.capitalize()}$filterIdentifiersCapitalized"
                    val appCenterAppTasks = mutableListOf<TaskProvider<out Task>>()

                    val outputFilePathApk =
                        apkVariant.packageApplicationProvider.get().outputDirectory.get().asFile
                    val outputFileAab =
                        //apkVariant.getFinalArtifact(InternalArtifactType.BUNDLE).get().asPath
                        File(
                            outputFilePathApk.path.replaceAfterLast(
                                "/outputs/",
                                "/bundle/${taskSuffix.decapitalize()}/"
                            )
                        )

                    appCenterAppTasks.add(
                        project.tasks.register(
                            "appCenterUploadApk$taskSuffix",
                            UploadAppCenterAppPackageTask::class.java,
                            UploadAppCenterAppPackageTask.PackageType.APK,
                            appCenterApp.apiToken,
                            appCenterApp.ownerName,
                            appCenterApp.appName,
                            appCenterApp.distributionGroups,
                            appCenterApp.notifyTesters,
                        appCenterApp.releaseNotes,
                            { File(outputFilePathApk, output.outputFileName) }
                    ).apply {
                        configure { task ->
                            task.group = APP_CENTER_PLUGIN_GROUP
                            task.description = "Upload ${apkVariant.name} APK to AppCenter"
                            task.mustRunAfter(assembleTask)
                        }
                    })

                    appCenterAppTasks.add(project.tasks.register(
                        "appCenterUploadAab$taskSuffix",
                        UploadAppCenterAppPackageTask::class.java,
                        UploadAppCenterAppPackageTask.PackageType.AAB,
                        appCenterApp.apiToken,
                        appCenterApp.ownerName,
                        appCenterApp.appName,
                        appCenterApp.distributionGroups,
                        appCenterApp.notifyTesters,
                        appCenterApp.releaseNotes,
                        {
                            File(
                                outputFileAab,
                                output.outputFileName.replace(".apk", ".aab")
                            )
                        }
                    ).apply {
                        configure { task ->
                            task.group = APP_CENTER_PLUGIN_GROUP
                            task.description = "Upload ${apkVariant.name} AAB to AppCenter"
                            task.mustRunAfter(bundleTask)
                        }
                    })
                    if (appCenterApp.uploadMappingFiles) {
                        appCenterAppTasks.add(project.tasks.register(
                            "appCenterUploadMapping$taskSuffix",
                            UploadAppCenterMappingTask::class.java,
                            appCenterApp.apiToken,
                            appCenterApp.ownerName,
                            appCenterApp.appName,
                            apkVariant.versionName,
                            apkVariant.versionCode,
                            { apkVariant.mappingFileProvider.get().first() }
                        ).apply {
                            configure { task ->
                                task.group = APP_CENTER_PLUGIN_GROUP
                                task.description = "Upload ${apkVariant.name} Mapping to AppCenter"
                                task.mustRunAfter(assembleTask)
                            }
                        })
                    }
                    if (appCenterApp.symbols.isNotEmpty()) {
                        appCenterAppTasks.add(project.tasks.register(
                            "appCenterUploadSymbols$taskSuffix",
                            UploadAppCenterSymbolsTask::class.java,
                            appCenterApp.apiToken,
                            appCenterApp.ownerName,
                            appCenterApp.appName,
                            apkVariant.versionName,
                            apkVariant.versionCode,
                            { appCenterApp.symbols }
                        ).apply {
                            configure { task ->
                                task.group = APP_CENTER_PLUGIN_GROUP
                                task.description = "Upload ${apkVariant.name} Symbols to AppCenter"
                                task.mustRunAfter(assembleTask)
                            }
                        })
                    }

                    project.tasks.register(
                        "appCenterUpload$taskSuffix"
                    ) { task ->
                        task.group = APP_CENTER_PLUGIN_GROUP
                        task.description = "Upload ${apkVariant.name} to AppCenter"
                        task.dependsOn(*(appCenterAppTasks.toTypedArray()))
                    }
                }
            }
        }
    }

    companion object {
        const val APP_CENTER_EXTENSION_NAME = "appcenter"
        const val APP_CENTER_PLUGIN_GROUP = "AppCenter"
    }
}
