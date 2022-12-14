package ru.mosgorpass.gradle.appcenter.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import ru.mosgorpass.gradle.appcenter.api.AppCenterUploaderFactory
import ru.mosgorpass.gradle.appcenter.utils.truncate
import java.io.File
import java.nio.file.Path
import javax.inject.Inject

open class UploadAppCenterApkTask @Inject constructor(
    @Input val apiToken: String,
    @Input val ownerName: String,
    @Input val appName: String,
    @Input val distributionGroups: List<String>,
    @Input val notifyTesters: Boolean,
    @Input val releaseNotes: Any?,
    @InputFile val fileProvider: () -> File
) : DefaultTask() {

    private var loggerFactory = services[ProgressLoggerFactory::class.java]

    @TaskAction
    fun upload() {
        val uploader = AppCenterUploaderFactory(project).create(apiToken, ownerName, appName)

        val loggerRelease = loggerFactory.newOperation("AppCenter")
        loggerRelease.start("AppCenter Upload apk", "Step 0/7")
        uploader.uploadApk(
            fileProvider(),
            toReleaseNotes(releaseNotes),
            distributionGroups,
            notifyTesters,
            UploadAppCenterAppPackageTask.PackageType.APK
        ) {
            loggerRelease.progress(it)
        }
        loggerRelease.completed("AppCenter Upload apk completed", false)
    }

    private fun toReleaseNotes(releaseNotes: Any?): String {
        return when (releaseNotes) {
            is File -> releaseNotes.readText()
            is Path -> releaseNotes.toFile().readText()
            is String -> releaseNotes
            else -> releaseNotes?.toString().orEmpty()
        }.truncate(MAX_RELEASE_NOTES_LENGTH)
    }

    companion object {
        const val MAX_RELEASE_NOTES_LENGTH = 5000
    }
}