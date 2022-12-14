package ru.mosgorpass.gradle.appcenter.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import ru.mosgorpass.gradle.appcenter.api.AppCenterUploaderFactory
import ru.mosgorpass.gradle.appcenter.utils.truncate
import java.io.File
import java.nio.file.Path
import javax.inject.Inject

open class UploadAppCenterAppPackageTask @Inject constructor(
    private val packageType: PackageType,
    private val apiToken: String,
    private val ownerName: String,
    private val appName: String,
    private val distributionGroups: List<String>,
    private val notifyTesters: Boolean,
    private val releaseNotes: Any?,
    private val fileProvider: () -> File
) : DefaultTask() {

    private var loggerFactory = services[ProgressLoggerFactory::class.java]

    @TaskAction
    fun upload() {
        val uploader = AppCenterUploaderFactory(project).create(apiToken, ownerName, appName)

        val loggerRelease = loggerFactory.newOperation("AppCenter")
        loggerRelease.start("AppCenter Upload $packageType", "Step 0/7")
        uploader.uploadApk(
            fileProvider(),
            toReleaseNotes(releaseNotes),
            distributionGroups,
            notifyTesters,
            packageType
        ) {
            loggerRelease.progress(it)
        }
        loggerRelease.completed("AppCenter Upload $packageType completed", false)
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

    enum class PackageType { APK, AAB }
}